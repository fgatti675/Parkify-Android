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

    if (parked_at_before === parked_at_now) return;

    console.log('Parked_at change: ', parked_at_before, parked_at_now);

    // You must return a Promise when performing asynchronous tasks inside a Functions such as
    return change.after.ref.collection("parking_events").add(parked_at_now);
  });
