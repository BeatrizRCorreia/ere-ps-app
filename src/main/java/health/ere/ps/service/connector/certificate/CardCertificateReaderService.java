package health.ere.ps.service.connector.certificate;

import de.gematik.ws.conn.certificateservice.v6.ReadCardCertificateResponse;
import de.gematik.ws.conn.certificateservicecommon.v2.X509DataInfoListType;
import de.gematik.ws.conn.connectorcommon.v5.Status;

import org.apache.commons.lang3.ArrayUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import health.ere.ps.exception.connector.ConnectorCardCertificateReadException;
import health.ere.ps.exception.idp.crypto.IdpCryptoException;
import health.ere.ps.model.idp.client.IdpTokenResult;
import health.ere.ps.model.idp.crypto.PkiIdentity;
import health.ere.ps.service.idp.crypto.CryptoLoader;

@ApplicationScoped
public class CardCertificateReaderService {
    @Inject
    CardCertReadExecutionService cardCertReadExecutionService;

    private static final String STATUS_OK = "OK";


    /**
     * Reads the AUT certificate of a card managed in the connector.
     *
     * @param invocationContext The context for the call to the connector.
     * @param cardHandle        The handle of the card.
     * @return The card's AUT certificate.
     */
    public byte[] readCardCertificate(InvocationContext invocationContext, String cardHandle)
            throws ConnectorCardCertificateReadException {
        byte[] x509Certificate = null;

        ReadCardCertificateResponse readCardCertificateResponse =
                cardCertReadExecutionService.doReadCardCertificate(invocationContext, cardHandle);

        Status status = readCardCertificateResponse.getStatus();
        if (status != null && status.getResult().equals(STATUS_OK)) {
            X509DataInfoListType x509DataInfoList = readCardCertificateResponse.getX509DataInfoList();
            List<X509DataInfoListType.X509DataInfo> x509DataInfos = x509DataInfoList.getX509DataInfo();
            if (x509DataInfos != null && !x509DataInfos.isEmpty()) {
                X509DataInfoListType.X509DataInfo x509DataInfo = x509DataInfos.get(0);
                x509Certificate = x509DataInfo.getX509Data().getX509Certificate();
            }
        }

        if(ArrayUtils.isEmpty(x509Certificate)) {
            throw new ConnectorCardCertificateReadException("Could not retrieve connector smart " +
                    "card certificate from the connector.");
        }

        return x509Certificate;
    }

    public byte[] readCardCertificate(String clientId, String clientSystem, String workplace,
                                      String cardHandle) throws ConnectorCardCertificateReadException {
        return readCardCertificate(new InvocationContext(clientId, clientSystem, workplace),
                cardHandle);
    }

    public byte[] readCardCertificate(String clientId, String clientSystem, String workplace,
                                      String userId, String cardHandle)
            throws ConnectorCardCertificateReadException {
        return readCardCertificate(new InvocationContext(clientId, clientSystem, workplace, userId),
                cardHandle);
    }

    public PkiIdentity retrieveCardCertIdentity(String clientId, String clientSystem,
                                                String workplace, String cardHandle,
                                                String connectorCertAuthPassword)
            throws ConnectorCardCertificateReadException, IdpCryptoException {
        byte[] connector_cert_auth = readCardCertificate(clientId, clientSystem, workplace,
                cardHandle);
        PkiIdentity identity;

        try (InputStream is = new ByteArrayInputStream(connector_cert_auth)) {
            identity = CryptoLoader.getIdentityFromP12(is, connectorCertAuthPassword);

        } catch (IOException e) {
           throw new ConnectorCardCertificateReadException("Error getting C_AUTH PKI Identity",
                   e);
        }

        return identity;
    }
}