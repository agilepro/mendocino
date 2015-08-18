/*
 * Copyright 2013 Keith D Swenson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.workcast.streams;

import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * It seems that the Java libraries for reading SSL connection have conflated
 * two different things: the ability to have an encrypted line, and the ability
 * to confirm who you are connecting to. Both are important, but the
 * implementation is that you can only have both. Any connection, instead of
 * giving you a simple option to check if the certificate on the other end is
 * valid, throws an exception preventing you from reading anything at all.
 *
 * Here is why this is so ridiculous. If you use HTTP and connect to a server,
 * you have no guarantee that the server is who it says it is, yet you can read
 * the bytes. If you would like to read those bytes in privacy, you would like
 * to use an SSL connection. It does not matter what key is used, just so long
 * as the line is encrypted then others will not be able to listen in. In
 * general it would be good to ensure that others can not listen in, especially
 * when you are passing passwords over the line.
 *
 * Yet, the default implementation is that the encrypted line is denied you if
 * you can't prove who the server is. But in the unincrypted case you didn't
 * know who the server was either. Regardless of whether you know what the real
 * identity of the server is that you are talking to, you are safer if the line
 * is encrypted.
 *
 * Here is the issue: anyone can generate a key and provide a secure line. It
 * should be a standard feature of all web servers. Basically, all web
 * interactions should be over SSL. But getting a certificate costs money,
 * because you have to have the infrastructure to verify if the certificate is
 * valid. While SSL might be free, a certificate will always cost money.
 *
 * By forcing you to either have both privacy and certificate at the same time,
 * effectively denies SSL privacy to those who do not have certificates!!
 * Certificates can only be gotten for well named servers, which are generally
 * in fixed location, and to known companies. But what about laptops? Getting a
 * certificate for a portable machine is not reasonable. Getting a certificate
 * for a virtual machine for testing is not reasonable. Yet, there are many
 * temporary virtual machines and laptops that run servers. But this is a
 * decision that causes a client to fail when talking to these servers.
 *
 * Clearly the validation of a certificate is "information content" of the
 * connect. It is a status flag that should be checkable. If the application
 * requires knowing that the certificate is valid, then it should have a way to
 * check it. But simply failing to connect is unnecessarily harsh. It assumes
 * too much about the needs of the client.
 *
 * So this class is implemented to disable the certificate validation checking.
 * This, too, goes too far in the other way, this will consider all certificates
 * valid whether they are or not. It would be far better to determine whether
 * the certificate is valid,and then allow the client to check, but I don't know
 * how to do this. This fix will allow Java clients to read data from HTTPS
 * servers which do not have certificates.
 *
 * Calling SSLPatch.disableSSLException will disable such validation in the
 * current VM until that VM is restarted.
 *
 * Keith D Swenson, October 12, 2011
 */
public class SSLPatch {

	/**
	 * Java proides a standard "trust manager" interface. This trust manager
	 * essentially disables the rejection of certificates by trusting anyone and
	 * everyone.
	 */
	public static X509TrustManager getDummyTrustManager() {
		return new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		};
	}

	/**
	 * Returns a hostname verifiers that always returns true, always positively
	 * verifies a host.
	 */
	public static HostnameVerifier getAllHostVerifier() {
		return new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
	}

	/**
	 * a call to disableSSLCertValidation will disable certificate validation
	 * for SSL connection made after this call. This is installed as the default
	 * in the JVM for future calls.
	 *
	 * Returns the properly initialized SSLContext in case it is needed for
	 * something else (like Apache HttpClient libraries) but if you don't need
	 * it you can ignore it.
	 */
	public static SSLContext disableSSLCertValidation() throws Exception {

		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { getDummyTrustManager() };

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(getAllHostVerifier());

		return sc;
	}

}
