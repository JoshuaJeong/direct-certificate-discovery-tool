package gov.hhs.onc.dcdt.crypto.utils;

import gov.hhs.onc.dcdt.crypto.CryptographyException;
import gov.hhs.onc.dcdt.crypto.DataEncoding;
import gov.hhs.onc.dcdt.crypto.PemType;
import gov.hhs.onc.dcdt.crypto.certs.CertificateType;
import gov.hhs.onc.dcdt.crypto.utils.CryptographyUtils.ToolProviderJcaJceHelper;
import gov.hhs.onc.dcdt.utils.ToolClassUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;

public abstract class CertificateUtils {
    public final static JcaX509CertificateConverter CERT_CONV = new JcaX509CertificateConverter();

    private final static int SERIAL_NUM_GEN_RAND_SEED_SIZE_DEFAULT = 8;

    static {
        CERT_CONV.setProvider(CryptographyUtils.PROVIDER);
    }

    public static BigInteger generateSerialNumber() throws CryptographyException {
        return BigInteger.valueOf(SecureRandomUtils.getRandom(SERIAL_NUM_GEN_RAND_SEED_SIZE_DEFAULT).nextLong()).abs();
    }

    public static X509Certificate readCertificate(InputStream inStream, CertificateType certType, DataEncoding dataEnc) throws CryptographyException {
        return readCertificate(CryptographyUtils.PROVIDER_HELPER, inStream, certType, dataEnc);
    }

    public static X509Certificate readCertificate(ToolProviderJcaJceHelper provHelper, InputStream inStream, CertificateType certType, DataEncoding dataEnc)
        throws CryptographyException {
        return readCertificate(provHelper, new InputStreamReader(inStream), certType, dataEnc);
    }

    public static X509Certificate readCertificate(Reader reader, CertificateType certType, DataEncoding dataEnc) throws CryptographyException {
        return readCertificate(CryptographyUtils.PROVIDER_HELPER, reader, certType, dataEnc);
    }

    public static X509Certificate readCertificate(ToolProviderJcaJceHelper provHelper, Reader reader, CertificateType certType, DataEncoding dataEnc)
        throws CryptographyException {
        try {
            return readCertificate(provHelper, IOUtils.toByteArray(reader), certType, dataEnc);
        } catch (IOException e) {
            throw new gov.hhs.onc.dcdt.crypto.certs.CertificateException(String.format(
                "Unable to read certificate instance of type (id=%s, providerName=%s) from reader (class=%s).", certType.getId(), provHelper.getProvider()
                    .getName(), ToolClassUtils.getName(reader)), e);
        }
    }

    public static X509Certificate readCertificate(byte[] data, CertificateType certType, DataEncoding dataEnc) throws CryptographyException {
        return readCertificate(CryptographyUtils.PROVIDER_HELPER, data, certType, dataEnc);
    }

    public static X509Certificate readCertificate(ToolProviderJcaJceHelper provHelper, byte[] data, CertificateType certType, DataEncoding dataEnc)
        throws CryptographyException {
        try {
            if (dataEnc == DataEncoding.PEM) {
                data = PemUtils.writePemContent(CryptographyUtils.findByType(PemType.class, certType.getType()), data);
            }

            return ((X509Certificate) getCertificateFactory(provHelper, certType).generateCertificate(new ByteArrayInputStream(data)));
        } catch (CertificateException e) {
            throw new gov.hhs.onc.dcdt.crypto.certs.CertificateException(String.format(
                "Unable to read certificate instance of type (id=%s, providerName=%s) from data.", certType.getId(), provHelper.getProvider().getName()), e);
        }
    }

    public static void writeCertificate(OutputStream outStream, X509Certificate cert, DataEncoding dataEnc) throws CryptographyException {
        writeCertificate(new OutputStreamWriter(outStream), cert, dataEnc);
    }

    public static void writeCertificate(Writer writer, X509Certificate cert, DataEncoding dataEnc) throws CryptographyException {
        try {
            IOUtils.write(writeCertificate(cert, dataEnc), writer);

            writer.flush();
        } catch (IOException e) {
            throw new gov.hhs.onc.dcdt.crypto.certs.CertificateException(String.format("Unable to write certificate instance (class=%s) to writer (class=%s).",
                ToolClassUtils.getClass(cert), ToolClassUtils.getName(writer)), e);
        }
    }

    public static byte[] writeCertificate(X509Certificate cert, DataEncoding dataEnc) throws CryptographyException {
        try {
            byte[] data = cert.getEncoded();

            return ((dataEnc == DataEncoding.DER) ? data : PemUtils.writePemContent(PemType.CERTIFICATE, data));
        } catch (CertificateEncodingException e) {
            throw new gov.hhs.onc.dcdt.crypto.certs.CertificateException(String.format("Unable to write certificate instance (class=%s) to data.",
                ToolClassUtils.getClass(cert)), e);
        }
    }

    public static CertificateFactory getCertificateFactory(CertificateType certType) throws CryptographyException {
        return getCertificateFactory(CryptographyUtils.PROVIDER_HELPER, certType);
    }

    public static CertificateFactory getCertificateFactory(ToolProviderJcaJceHelper provHelper, CertificateType certType) throws CryptographyException {
        try {
            return provHelper.createCertificateFactory(certType.getId());
        } catch (CertificateException e) {
            throw new gov.hhs.onc.dcdt.crypto.certs.CertificateException(String.format(
                "Unable to get certificate factory instance for certificate type (id=%s, providerName=%s).", certType.getId(), provHelper.getProvider()
                    .getName()), e);
        }
    }
}
