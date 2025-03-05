# CreateSamlAssertion

This is a example project to create a SAML ASeertion and signing it ussing X509 Certificate.
This project uses java 17 and Spring boot 3.3.5 versions

First instal open SSH in your system.
"https://www.youtube.com/watch?v=oX0Et0RtxoM" This video shows how to. 

Use the below command to create Key and certificate
openssl req -nodes -x509 -sha256 -newkey rsa:2048 -keyout private.pem -out certificate.pem 

You can also refer this Site for more information 
"https://help.sap.com/docs/SAP_SUCCESSFACTORS_PLATFORM/d599f15995d348a1b45ba5603e2aba9b/637fdad7a8ae44e3b9f00e7504045eb3.html"
