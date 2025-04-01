package com.noctusoft.webviewbrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.noctusoft.webviewbrowser.model.Credentials;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Manager class for securely storing and retrieving credentials.
 */
public class CredentialsManager {
    private static final String TAG = "CredentialsManager";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "WebViewBrowserCredentialsKey";
    private static final String SHARED_PREFS_NAME = "WebViewBrowserCredentials";
    private static final String DOMAINS_KEY = "domains";
    private static CredentialsManager instance;

    private final Context context;
    private final SharedPreferences sharedPreferences;

    /**
     * Gets the singleton instance of CredentialsManager.
     *
     * @param context The application context.
     * @return The CredentialsManager instance.
     */
    public static synchronized CredentialsManager getInstance(Context context) {
        if (instance == null) {
            instance = new CredentialsManager(context.getApplicationContext());
        }
        return instance;
    }

    private CredentialsManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        try {
            createSecretKey();
        } catch (Exception e) {
            Log.e(TAG, "Error creating secret key", e);
        }
    }

    /**
     * Saves credentials for a domain.
     *
     * @param username The username.
     * @param password The password.
     * @param domain The domain for which to save the credentials.
     * @return True if the credentials were saved successfully, false otherwise.
     */
    public boolean saveCredentials(String username, String password, String domain) {
        try {
            // Create credentials object
            Credentials credentials = new Credentials(username, password);

            // Serialize credentials
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(credentials);
            oos.close();
            byte[] serializedCredentials = baos.toByteArray();

            // Encrypt credentials
            byte[] encryptedCredentials = encrypt(serializedCredentials);

            // Store encrypted credentials
            String encodedCredentials = Base64.encodeToString(encryptedCredentials, Base64.DEFAULT);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(domain, encodedCredentials);

            // Add domain to list of domains if not already present
            List<String> domains = getAllDomains();
            if (!domains.contains(domain)) {
                domains.add(domain);
                editor.putString(DOMAINS_KEY, String.join(",", domains));
            }
            
            editor.apply();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving credentials for " + domain, e);
            return false;
        }
    }

    /**
     * Loads credentials for a domain.
     *
     * @param domain The domain for which to load the credentials.
     * @return The credentials, or null if no credentials were found or an error occurred.
     */
    public Credentials loadCredentials(String domain) {
        try {
            String encodedCredentials = sharedPreferences.getString(domain, null);
            if (encodedCredentials == null) {
                return null;
            }

            byte[] encryptedCredentials = Base64.decode(encodedCredentials, Base64.DEFAULT);
            byte[] decryptedBytes = decrypt(encryptedCredentials);

            ByteArrayInputStream bais = new ByteArrayInputStream(decryptedBytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (Credentials) ois.readObject();
        } catch (Exception e) {
            Log.e(TAG, "Error loading credentials for " + domain, e);
            return null;
        }
    }

    /**
     * Deletes credentials for a domain.
     *
     * @param domain The domain for which to delete the credentials.
     * @return True if the credentials were deleted successfully, false otherwise.
     */
    public boolean deleteCredentials(String domain) {
        try {
            // Remove credentials for the domain
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(domain);

            // Update list of domains
            List<String> domains = getAllDomains();
            domains.remove(domain);
            editor.putString(DOMAINS_KEY, String.join(",", domains));
            
            editor.apply();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting credentials for " + domain, e);
            return false;
        }
    }

    /**
     * Gets all domains for which credentials are stored.
     *
     * @return A list of domains.
     */
    public List<String> getAllDomains() {
        String domainsString = sharedPreferences.getString(DOMAINS_KEY, "");
        List<String> domains = new ArrayList<>();
        
        if (!domainsString.isEmpty()) {
            String[] domainArray = domainsString.split(",");
            for (String domain : domainArray) {
                domains.add(domain);
            }
        }
        
        return domains;
    }

    /**
     * Creates a secret key in the Android KeyStore.
     */
    private void createSecretKey() throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            
            // Check if the key already exists
            if (keyStore.containsAlias(KEY_ALIAS)) {
                return;
            }
        } catch (KeyStoreException | CertificateException | IOException e) {
            Log.e(TAG, "Error accessing KeyStore", e);
            return;
        }

        // Create new key
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build();
        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }

    /**
     * Gets the secret key from the Android KeyStore.
     *
     * @return The secret key.
     */
    private SecretKey getSecretKey() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    /**
     * Encrypts data using the secret key.
     *
     * @param data The data to encrypt.
     * @return The encrypted data.
     */
    private byte[] encrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, KeyStoreException, CertificateException, IOException,
            UnrecoverableEntryException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
        byte[] iv = cipher.getIV();
        byte[] encryptedData = cipher.doFinal(data);
        
        // Combine IV and encrypted data
        byte[] combined = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);
        
        return combined;
    }

    /**
     * Decrypts data using the secret key.
     *
     * @param data The data to decrypt.
     * @return The decrypted data.
     */
    private byte[] decrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, KeyStoreException, CertificateException, IOException,
            UnrecoverableEntryException {
        // Extract IV from the beginning of the data
        byte[] iv = new byte[12]; // GCM default IV size
        byte[] encryptedData = new byte[data.length - iv.length];
        System.arraycopy(data, 0, iv, 0, iv.length);
        System.arraycopy(data, iv.length, encryptedData, 0, encryptedData.length);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec);
        
        return cipher.doFinal(encryptedData);
    }
}
