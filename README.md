# CloudFoundry_AppUse_Export
App that exports the data from app_use_events in cloud foundry to CSV file.


Once you clone the repo - use Maven and run 
mvn clean package

This will create the target folder and jar file specified in the manifest.

Edit the manifest.yml:

    CF_APIHOST: {add your CAPI API url}
    CF_USERNAME: {Add the CF UAA username you want to use to access the API}
    CF_PASSWORD: {Above UAA user's password}
    
If you are using a self signed certificate in CF set the CF_SKIPSSL to true, otherwise set as false.

CF push the app

