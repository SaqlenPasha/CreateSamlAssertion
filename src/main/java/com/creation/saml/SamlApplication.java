package com.creation.saml;

import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyName;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.PGPData;
import javax.xml.crypto.dsig.keyinfo.RetrievalMethod;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.keyinfo.X509IssuerSerial;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opensaml.core.config.Configuration;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.impl.BasicKeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureSupport;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


@SpringBootApplication
public class SamlApplication {

	public static void main(String[] args) throws Exception {
		
		SamlApplication saml = new SamlApplication();
		InitializationService.initialize();

		Assertion assertion=saml.buildSAMLAssertion("{Insert Your API Key which is be provided} ","{Path of the private key generated by the command in the readme file}","{Path of the certificate generated by the command in the readme file}");
		saml.getAccessToken(assertion);
	}
	

	    public Assertion buildSAMLAssertion(String apiKey, String privateKey, String certificate) throws Exception {
	        // Create the assertion

			XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
	    	
	    	Assertion assertion = (Assertion) builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME)
	                .buildObject(Assertion.DEFAULT_ELEMENT_NAME);
	        assertion.setID(UUID.randomUUID().toString());
	        assertion.setIssueInstant(Instant.now());
	        assertion.setVersion(SAMLVersion.VERSION_20);

	        // Add Issuer
	        Issuer issuer = (Issuer) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME)
	                .buildObject(Issuer.DEFAULT_ELEMENT_NAME);

	        issuer.setValue("www.anything.com");    // This can be anything.
	        assertion.setIssuer(issuer);

	        // Add Subject
	        Subject subject = createSubject();
	        assertion.setSubject(subject);

	        // Add Conditions
	        Conditions conditions = createConditions();
	        assertion.setConditions(conditions);

	        // Add AuthnStatement
	        AuthnStatement authnStatement = createAuthnStatement();
	        assertion.getAuthnStatements().add(authnStatement);

	        // Add AttributeStatement
	        AttributeStatement attributeStatement = createAttributeStatement(apiKey);
	        assertion.getAttributeStatements().add(attributeStatement);

	        // Sign the assertion
	        signAssertion(assertion, privateKey, certificate);
	        
		    String samlAssertion=convertAssertionToString(assertion);
		    System.out.println(samlAssertion);
	        printAssertion(assertion);
	        return assertion;
	    }

	    private static Subject createSubject() {
			
	    	XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();

	    	
	        NameID nameID = (NameID) builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME)
	                .buildObject(NameID.DEFAULT_ELEMENT_NAME);
	        nameID.setValue("{Name_Id}");                                                       // Name Id of SF which is provided by the client
	        nameID.setFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");

	        SubjectConfirmationData confirmationData = (SubjectConfirmationData) builderFactory
	                .getBuilder(SubjectConfirmationData.DEFAULT_ELEMENT_NAME)
	                .buildObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
	        confirmationData.setNotOnOrAfter(Instant.now().plusSeconds(600));
	        confirmationData.setRecipient("https://api5.successfactors.eu/oauth/token");    


	        SubjectConfirmation confirmation = (SubjectConfirmation) builderFactory
	                .getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME)
	                .buildObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
	        confirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
	        confirmation.setSubjectConfirmationData(confirmationData);

	        Subject subject = (Subject) builderFactory.getBuilder(Subject.DEFAULT_ELEMENT_NAME)
	                .buildObject(Subject.DEFAULT_ELEMENT_NAME);
	        subject.setNameID(nameID);
	        subject.getSubjectConfirmations().add(confirmation);

	        return subject;
	    }

	    private static Conditions createConditions() {
	    	
			XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
	    	
	        Audience audience = (Audience) builderFactory.getBuilder(Audience.DEFAULT_ELEMENT_NAME)
	                .buildObject(Audience.DEFAULT_ELEMENT_NAME);
	        audience.setAudienceURI("www.successfactors.com");

	        AudienceRestriction audienceRestriction = (AudienceRestriction) builderFactory
	                .getBuilder(AudienceRestriction.DEFAULT_ELEMENT_NAME)
	                .buildObject(AudienceRestriction.DEFAULT_ELEMENT_NAME);
	        audienceRestriction.getAudiences().add(audience);

	        Conditions conditions = (Conditions) builderFactory.getBuilder(Conditions.DEFAULT_ELEMENT_NAME)
	                .buildObject(Conditions.DEFAULT_ELEMENT_NAME);
	        conditions.setNotBefore(Instant.now().minusSeconds(600));
	        conditions.setNotOnOrAfter(Instant.now().plusSeconds(600));
	        conditions.getAudienceRestrictions().add(audienceRestriction);

	        return conditions;
	    }

	    private static AuthnStatement createAuthnStatement() {
	    	
			XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
	    	
	    	
	        AuthnContextClassRef authnContextClassRef = (AuthnContextClassRef) builderFactory
	                .getBuilder(AuthnContextClassRef.DEFAULT_ELEMENT_NAME)
	                .buildObject(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
	        authnContextClassRef.setURI(AuthnContext.PPT_AUTHN_CTX);

	        AuthnContext authnContext = (AuthnContext) builderFactory.getBuilder(AuthnContext.DEFAULT_ELEMENT_NAME)
	                .buildObject(AuthnContext.DEFAULT_ELEMENT_NAME);
	        authnContext.setAuthnContextClassRef(authnContextClassRef);

	        AuthnStatement authnStatement = (AuthnStatement) builderFactory.getBuilder(AuthnStatement.DEFAULT_ELEMENT_NAME)
	                .buildObject(AuthnStatement.DEFAULT_ELEMENT_NAME);
	        authnStatement.setAuthnInstant(Instant.now());
	        authnStatement.setAuthnContext(authnContext);
	        authnStatement.setSessionIndex(UUID.randomUUID().toString());

	        return authnStatement;
	    }

	    private static AttributeStatement createAttributeStatement(String apiKey) {    // You might need to add attributes based on your requirement
	    	
			XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
	    	
	        Attribute attribute = (Attribute) builderFactory.getBuilder(Attribute.DEFAULT_ELEMENT_NAME)
	                .buildObject(Attribute.DEFAULT_ELEMENT_NAME);
	        attribute.setName("api_key");

	        XSString attributeValue = (XSString) builderFactory.getBuilder(XSString.TYPE_NAME)
	                .buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
	        attributeValue.setValue(apiKey);

	        attribute.getAttributeValues().add(attributeValue);

	        AttributeStatement attributeStatement = (AttributeStatement) builderFactory
	                .getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME)
	                .buildObject(AttributeStatement.DEFAULT_ELEMENT_NAME);
	        attributeStatement.getAttributes().add(attribute);

	        return attributeStatement;
	    }

	    private static void signAssertion(Assertion assertion, String privateKey, String certificate) throws Exception {
	        // Load private key and certificate
	    	
			XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
	    	
	        BasicX509Credential credential = new BasicX509Credential(loadCertificate(certificate), loadPrivateKey(privateKey));

	        credential.setPrivateKey(loadPrivateKey(privateKey));
	        credential.setEntityCertificate(loadCertificate(certificate));
	        
	        // Create Signature
	        Signature signature = (Signature) builderFactory.getBuilder(Signature.DEFAULT_ELEMENT_NAME)
	                .buildObject(Signature.DEFAULT_ELEMENT_NAME);
	        signature.setSigningCredential(credential);
	        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
	        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
	        
	        
			BasicKeyInfoGeneratorFactory keyInfoGeneratorFactory = new BasicKeyInfoGeneratorFactory();

	        KeyInfoGenerator keyInfoGenerator = keyInfoGeneratorFactory.newInstance();
	        signature.setKeyInfo(keyInfoGenerator.generate(credential));
	    
	        // Attach the Signature to the Assertion
	        assertion.setSignature(signature);

	        // Marshall and Sign
	        XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(assertion).marshall(assertion);
	        Signer.signObject(signature);
	    }
	    
	public String getAccessToken(Assertion assertionObj) {
	    String tokenUrl = "{API which is used to get the access token}";
	    RestTemplate restTemplate = new RestTemplate();

	    String samlAssertion=convertAssertionToString(assertionObj);
	    System.out.println(samlAssertion);
	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

	    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
	    body.add("grant_type", "urn:ietf:params:oauth:grant-type:saml2-bearer");
	    body.add("assertion", samlAssertion);
	    body.add("client_id", "{Client Id  i.e, API key}");
	    body.add("company_id", "{Company Id}");
	    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

	    ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

	    if (response.getStatusCode() == HttpStatus.OK) {
	        return response.getBody();
	    } else {
	        throw new RuntimeException("Failed to retrieve token: " + response.getStatusCode());
	    }

	}
	
	
    private static String convertAssertionToString(Assertion assertion) {
        try {
            Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(assertion);
            Element element = marshaller.marshall(assertion);
            StringWriter writer = new StringWriter();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return Base64.getEncoder().encodeToString(writer.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Assertion to String", e);
        }
    }
	
	public static PrivateKey loadPrivateKey(String filename) throws Exception {
		String key = new String(Files.readAllBytes(Paths.get(filename)));
		key = key.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s",
				"");
		byte[] keyBytes = Base64.getDecoder().decode(key);
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	public static X509Certificate loadCertificate(String filename) throws Exception {
		CertificateFactory fact = CertificateFactory.getInstance("X.509");
		return (X509Certificate) fact.generateCertificate(Files.newInputStream(Paths.get(filename)));
	}

	
	private void printAssertion(Assertion assertion) throws TransformerException {
		MarshallerFactory marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
		Marshaller marshaller = marshallerFactory.getMarshaller(assertion);
		if (marshaller == null) {
			throw new RuntimeException("No marshaller found for SAML Assertion.");
		}

		try {
			Element assertionElement = marshaller.marshall(assertion);
			// Convert the XML element to a String and print it
			String assertionXml = convertElementToString(assertionElement);
			System.out.println("SAML Assertion: " + assertionXml);
		} catch (MarshallingException e) {
			e.printStackTrace();
		}
	}
	
    private static String convertElementToString(Element element) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(element), new StreamResult(writer));
        return writer.getBuffer().toString();
    }



// Ignore the below code :

	public void createSAMLAssertion() throws Exception {
		Assertion assertion=createSignedAssertion(loadPrivateKey("=--"),loadCertificate("--"));
		getAccessToken(assertion);
	}

	public Assertion createSignedAssertion(PrivateKey privateKey, X509Certificate certificate) throws Exception {
		Assertion assertion = createAssertion();
		printAssertion(assertion);
		
		BasicX509Credential credential = new BasicX509Credential((X509Certificate) certificate, privateKey);
		credential.setEntityCertificate((X509Certificate) certificate);

		Signature signature = (Signature) XMLObjectProviderRegistrySupport.getBuilderFactory()
		    .getBuilder(Signature.DEFAULT_ELEMENT_NAME)
		    .buildObject(Signature.DEFAULT_ELEMENT_NAME);

		signature.setSigningCredential(credential);
		signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
		signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

		SignatureSigningParameters signingParameters = new SignatureSigningParameters();
		signingParameters.setSigningCredential(credential);
		signingParameters.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
		signingParameters.setSignatureCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

		SignatureSupport.prepareSignatureParams(signature, signingParameters);
		
		assertion.setSignature(signature);
		
		System.out.println("<--------------------After Signature-------------------->");
		printAssertion(assertion);
		
		return assertion;
	}

	
	private Assertion createAssertion() throws InitializationException {
		
		InitializationService.initialize();
		
		XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();

        // 1. Create Issuer
        Issuer issuer = (Issuer) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME)
                .buildObject(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue("www.anything.com");

        // 2. Create NameID
        NameID nameID = (NameID) builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME)
                .buildObject(NameID.DEFAULT_ELEMENT_NAME);
        nameID.setValue("{Name Id}");
        nameID.setFormat(NameIDType.UNSPECIFIED);  // You can change this based on your requirement

        // 3. Create Subject
        Subject subject = (Subject) builderFactory.getBuilder(Subject.DEFAULT_ELEMENT_NAME)
                .buildObject(Subject.DEFAULT_ELEMENT_NAME);

        SubjectConfirmationData confirmationData = (SubjectConfirmationData) builderFactory
                .getBuilder(SubjectConfirmationData.DEFAULT_ELEMENT_NAME)
                .buildObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
        confirmationData.setRecipient("https://api5.successfactors.eu/oauth/token");
        ZonedDateTime now = ZonedDateTime.now(java.time.ZoneOffset.UTC);
        Instant notOnOrAfter = now.plus(10, ChronoUnit.MINUTES).toInstant();


        confirmationData.setNotOnOrAfter(notOnOrAfter);

        SubjectConfirmation subjectConfirmation = (SubjectConfirmation) builderFactory
                .getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME)
                .buildObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        subjectConfirmation.setSubjectConfirmationData(confirmationData);

        subject.setNameID(nameID);
        subject.getSubjectConfirmations().add(subjectConfirmation);

        // 4. Create Conditions with AudienceRestriction
        Conditions conditions = (Conditions) builderFactory.getBuilder(Conditions.DEFAULT_ELEMENT_NAME)
                .buildObject(Conditions.DEFAULT_ELEMENT_NAME);

        Audience audience = (Audience) builderFactory.getBuilder(Audience.DEFAULT_ELEMENT_NAME)
                .buildObject(Audience.DEFAULT_ELEMENT_NAME);
        audience.setAudienceURI("www.successfactors.com");

        AudienceRestriction audienceRestriction = (AudienceRestriction) builderFactory
                .getBuilder(AudienceRestriction.DEFAULT_ELEMENT_NAME)
                .buildObject(AudienceRestriction.DEFAULT_ELEMENT_NAME);
        audienceRestriction.getAudiences().add(audience);

        conditions.getAudienceRestrictions().add(audienceRestriction);
        
        Instant notBefore = now.minus(10, ChronoUnit.MINUTES).toInstant();
        Instant notOnAfter = now.plus(10, ChronoUnit.MINUTES).toInstant();
        
        conditions.setNotBefore(notBefore);
        conditions.setNotOnOrAfter(notOnAfter);

        // 5. Create AttributeStatement with API_key
        AttributeStatement attributeStatement = (AttributeStatement) builderFactory
                .getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME)
                .buildObject(AttributeStatement.DEFAULT_ELEMENT_NAME);

        Attribute apiKeyAttribute = (Attribute) builderFactory.getBuilder(Attribute.DEFAULT_ELEMENT_NAME)
                .buildObject(Attribute.DEFAULT_ELEMENT_NAME);
        apiKeyAttribute.setName("api_key");

        XSString attributeValue = (XSString) builderFactory.getBuilder(XSString.TYPE_NAME)
                .buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        attributeValue.setValue("Api_Key");

        apiKeyAttribute.getAttributeValues().add(attributeValue);
        attributeStatement.getAttributes().add(apiKeyAttribute);

        // 6. Create Assertion
        Assertion assertion = (Assertion) builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME)
                .buildObject(Assertion.DEFAULT_ELEMENT_NAME);
        assertion.setID("_" + UUID.randomUUID().toString());
        assertion.setIssueInstant(notBefore);
        assertion.setVersion(SAMLVersion.VERSION_20);

        assertion.setIssuer(issuer);
        assertion.setSubject(subject);
        assertion.setConditions(conditions);
        assertion.getAttributeStatements().add(attributeStatement);

        return assertion;
		
	}    
}
