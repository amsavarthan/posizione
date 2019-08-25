'use strict';

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

//Triggers when a user gets a new tracker and sends a notification.

exports.sendNotification = functions.database.ref('/users/{userUid}/friends/{uniqueId}')
    .onWrite(async(change, context) => {
		
  	const userUid=context.params.userUid;
    const uniqueId=context.params.uniqueId;

  	console.log('function started');
  if (!change.after.exists()){
              //Exit when the data is deleted
                return console.log('data deleted');
            }
  
    const getUserProfile = admin.database().ref(`/users/${userUid}`).once('value');
  return getUserProfile.then((result)=>{
    
    const adder_name=result.val().name;
    const adder_pic=result.val().image;
    const adder_token=result.val().token;

    let query=admin.database().ref('/users').orderByChild('unique_id').equalTo(uniqueId);
	return query.once('value',(snapshot)=>{
			
		snapshot.forEach((child)=>{
          
          	const added_name=child.val().name;
    		const added_pic=child.val().image;
    		const added_token=child.val().token;
                
			// Send added notification
			if(!change.before.exists()){
				if (change.after.val()==true){
				 console.log('send added notificaition');
                 
                 const payload = {
                   data: {
                     title: "New Tracker",
       	 			 body: adder_name+" added you to his tracking list.",
           		     image: adder_pic,
		             action: "com.amsavarthan.posizione.NEW_TRACKER"
      	            }
                 };

               return admin.messaging().sendToDevice(added_token, payload).then(function(response){
                 console.log('Notification sent successfully:',response);
		       }) 
		       .catch(function(error){
                 console.log('Notification sent failed:',error);
	           });      
                  
                  
				}else{
                  
                  const payload = {
                   data: {
                     title: "New Request",
       	   			 body: adder_name+" sent you request.",
                     image: adder_pic,
		             action: "com.amsavarthan.posizione.NEW_REQUEST"
      	            }
                 };

               return admin.messaging().sendToDevice(added_token, payload).then(function(response){
                 console.log('Notification sent successfully:',response);
		       }) 
		       .catch(function(error){
                 console.log('Notification sent failed:',error);
	           });     
                  
				 
				}
			}
  
   	     	// previous true and current false means Send revoked notification
  	     	
  		 	if(change.before.val()==true && change.after.val()==false){
   	      		console.log('send revoked notification');
              
              /*const payload = {
                   data: {
                     title: "Request revoked",
       	 			 body: added_name+" revoked your request for tracking them.",
           		     image: added_pic,
		             action: "com.amsavarthan.posizione.REQUEST_REVOKED"
      	            }
                 };

               return admin.messaging().sendToDevice(adder_token, payload).then(function(response){
                 console.log('Notification sent successfully:',response);
		       }) 
		       .catch(function(error){
                 console.log('Notification sent failed:',error);
	           });      
             */
    		 }
          
           // previous false and current true means Send accepted notification
          
            else{
   	      		console.log('send accepted notification');
              
               const payload = {
                   data: {
                     title: "Request accepted",
       	 			 body: added_name+" accepted your track request.",
           		     image: added_pic,
		             action: "com.amsavarthan.posizione.NEW_TRACKER"
      	            }
                 };

               return admin.messaging().sendToDevice(adder_token, payload).then(function(response){
                 console.log('Notification sent successfully:',response);
		       }) 
		       .catch(function(error){
                 console.log('Notification sent failed:',error);
	           });      
              
    	 	 }
          
     	});
		
    });
    
  });

});

