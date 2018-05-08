const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp({
    credential: admin.credential.applicationDefault()
});

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });

exports.onCarChangedLocation = functions.firestore
  .document('/cars/{carId}')
  .onUpdate((change, context) => {
    // Get an object representing the document
    const newValue = change.after.data();
    // ...or the previous value before this update
    const previousValue = change.before.data();

    // access a particular field as you would any JS property
    const parked_at_now = newValue.parked_at;
    const parked_at_before = previousValue.parked_at;


    if (!parked_at_now || !parked_at_now.location) {
        console.log('No new parking location');
        return null;
    }

    if (parked_at_before && parked_at_before.location
            && parked_at_before.location._latitude === parked_at_now.location._latitude
            && parked_at_before.location._longitude === parked_at_now.location._longitude
        ) {
        console.log('Parking location didn\'t change: ', parked_at_before, parked_at_now);
        return null;
    }

    console.log('Parked_at change: ', parked_at_before, parked_at_now);

    return change.after.ref.collection("parking_events").add(parked_at_now);

  });

exports.onFreedSpot = functions.firestore
    .document('/freed_spots/{spotId}')
    .onCreate((snap, context) => {
        const newParkingSpot = snap.data();

        newParkingSpot['expires_at'] = new Date(newParkingSpot.time.getTime() + 15 * 60 * 1000) ;

        console.log('New freed spot', context.params.pushId, newParkingSpot);

        return snap.ref.firestore.collection('available_spots_live').add(newParkingSpot);
    });


function deleteQueryBatch(db, query, batchSize, resolve, reject) {
    query.get()
        .then((snapshot) => {
          // When there are no documents left, we are done
          if (snapshot.size === 0) {
            return 0;
          }

          // Delete documents in a batch
          var batch = db.batch();
          snapshot.docs.forEach((doc) => {
            batch.delete(doc.ref);
          });

          return batch.commit().then(() => {
            return snapshot.size;
          });
        }).then((numDeleted) => {

            console.log("Deleted stale parking spots: " + numDeleted);

          if (numDeleted === 0) {
            resolve();
            return numDeleted;
          }

          // Recurse on the next process tick, to avoid
          // exploding the stack.
          process.nextTick(() => {
            deleteQueryBatch(db, query, batchSize, resolve, reject);
          });
          return numDeleted;
        })
        .catch(reject);
}

//const ref = admin.firestore().ref();
exports.minute_job =
    functions.pubsub.topic('minute-tick').onPublish((event) => {
        var db = admin.firestore();
        var liveSpots = db.collection('available_spots_live');
        var batchSize = 500;
        var query = liveSpots
            .where('expires_at', '<', new Date())
            .limit(batchSize);

        console.log("This job is ran every minute!");
        return new Promise((resolve, reject) => {
          deleteQueryBatch(db, query, batchSize, resolve, reject);
        });
    });

