package com.example.ruixuandai.zenfacedigit;

/**
 * Created by ruixuandai on 11/21/17.
 */



import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Regions;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;


public class AwsIoTMQTT {
    private static final String TAG = "AwsIoTMQTT";

//    // --- Constants to modify per your configuration ---
//
//    // IoT endpoint
//    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
//    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a3i6ug6av1nel4.iot.us-east-1.amazonaws.com";
//    // Region of AWS IoT
//    private static final Regions MY_REGION = Regions.US_EAST_1;
//    // Filename of KeyStore file on the filesystem
//    private static final String KEYSTORE_NAME = "iot_keystore";
//    // Password for the private key in the KeyStore
//    private static final String KEYSTORE_PASSWORD = "5253419";
//    // Certificate and key aliases in the KeyStore
//    private static final String CERTIFICATE_ID = "awskey";
//    AWSIotMqttManager mqttManager;
//
//    private static final String COGNITO_POOL_ID = "CHANGE_ME";
//
//    // Name of the AWS IoT policy to attach to a newly created certificate
//    private static final String AWS_IOT_POLICY_NAME = "CHANGE_ME";



    // IoT endpoint
// AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a10qe38noifilm.iot.us-east-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
// AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "us-east-1:66215a0d-79cd-4ecc-9dc0-b2ace9068787";
    // Name of the AWS IoT policy to attach to a newly created certificate
    private static final String AWS_IOT_POLICY_NAME = "mqtt_gps_uploading";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_EAST_1;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";


    private AWSIotMqttManager mqttManager;
    private String clientId ;
    private int connectionStatus = -1;



    private KeyStore clientKeyStore = null;
    private String certificateId;
    private CognitoCachingCredentialsProvider credentialsProvider;

    private AWSIotClient mIotAndroidClient;
    private String keystorePath;
    private String keystoreName;
    private String keystorePassword;


    public AwsIoTMQTT(Context context){
        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = Build.DEVICE+ "/" + Build.SERIAL;

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                context, // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );


        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = context.getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                } else {
                    Log.i(TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);


                    } catch (Exception e) {
                        Log.e(TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();

            connect();

        }
    }


    public void connect(){
        try {
            mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(TAG, "Status = " + String.valueOf(status));
                    if (status == AWSIotMqttClientStatus.Connecting) {
                        connectionStatus = 2;

                    } else if (status == AWSIotMqttClientStatus.Connected) {
                        connectionStatus = 1;
                        publishString("Connected:" + System.currentTimeMillis(), "zenwatch");

                    } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                        if (throwable != null) {
                            Log.e(TAG, "Connection error.", throwable);
                        }
                        connectionStatus = 3;
                    } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                        if (throwable != null) {
                            Log.e(TAG, "Connection error.", throwable);
                        }
                        connectionStatus = -1;
                    } else {
                        connectionStatus = -1;

                    }
                }
            });
        } catch (final Exception e) {
            Log.e(TAG, "Connection error.", e);

        }
    }


    public void publishString(String str, String topic){
        try {
            mqttManager.publishString(str, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(TAG, "Publish error.", e);
        }
    }


}