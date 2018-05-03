const functions = require('firebase-functions');

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


exports.minute_job =
  functions.pubsub.topic('minute-tick').onPublish((event) => {
    console.log("This job is ran every minute!");
    return null;
  });

