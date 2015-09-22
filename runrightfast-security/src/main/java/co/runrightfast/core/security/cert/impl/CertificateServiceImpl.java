/*
 Copyright 2015 Alfio Zappala

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package co.runrightfast.core.security.cert.impl;

import static co.runrightfast.core.security.BouncyCastle.BOUNCY_CASTLE;
import static co.runrightfast.core.security.SignatureAlgorithm.SHA512withRSA;
import co.runrightfast.core.security.cert.CertificateService;
import co.runrightfast.core.security.cert.CertificateServiceException;
import co.runrightfast.core.security.cert.SelfSignedX509V1CertRequest;
import co.runrightfast.core.security.cert.X509V1CertRequest;
import static co.runrightfast.core.security.cert.X509V1CertRequestToX509v1CertificateBuilder.x509v1CertificateBuilder;
import static co.runrightfast.core.security.util.SecurityUtils.strongSecureRandom;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import lombok.NonNull;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 *
 * @author alfio
 */
public class CertificateServiceImpl implements CertificateService {

    @Override
    public X509Certificate generateX509CertificateV1(@NonNull final X509V1CertRequest request, @NonNull final PrivateKey privateKey) {
        final ContentSigner signer = contentSigner(privateKey);
        final X509v1CertificateBuilder certBuilder = x509v1CertificateBuilder(request);
        final X509CertificateHolder certHolder = certBuilder.build(signer);
        try {
            return new JcaX509CertificateConverter()
                    .setProvider(BOUNCY_CASTLE)
                    .getCertificate(certHolder);
        } catch (final CertificateException ex) {
            throw new CertificateServiceException(ex);
        }
    }

    private ContentSigner contentSigner(final PrivateKey privateKey) {
        try {
            return new JcaContentSignerBuilder(SHA512withRSA.name())
                    .setProvider(BOUNCY_CASTLE)
                    .setSecureRandom(strongSecureRandom())
                    .build(privateKey);
        } catch (final OperatorCreationException ex) {
            throw new CertificateServiceException(ex);
        }
    }

    @Override
    public X509Certificate generateSelfSignedX509CertificateV1(@NonNull final SelfSignedX509V1CertRequest request) throws CertificateServiceException {
        return generateX509CertificateV1(request.toX509V1CertRequest(), request.getKeyPair().getPrivate());

    }

}