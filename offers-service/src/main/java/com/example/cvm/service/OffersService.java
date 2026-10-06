package com.example.cvm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.cvm.client.KongRecommendationClient;
import com.example.cvm.client.KongTokenProvider;
import com.example.cvm.config.KongProperties;
import com.example.cvm.dto.GetOffersResponse;
import com.example.cvm.dto.KongRequest;
import com.example.cvm.dto.KongResponse;
import com.example.cvm.exception.TokenRejectedException;
import com.example.cvm.mapper.OfferMapper;
import com.example.cvm.util.TransIdGenerator;

@Service
public class OffersService {

    private static final Logger log = LoggerFactory.getLogger(OffersService.class);

    private final KongTokenProvider tokenProvider;
    private final KongRecommendationClient kongClient;
    private final OfferMapper mapper;
    private final TransIdGenerator transIdGenerator;
    private final KongProperties props;

    public OffersService(KongTokenProvider tokenProvider, KongRecommendationClient kongClient,
                         OfferMapper mapper, TransIdGenerator transIdGenerator, KongProperties props) {
        this.tokenProvider = tokenProvider;
        this.kongClient = kongClient;
        this.mapper = mapper;
        this.transIdGenerator = transIdGenerator;
        this.props = props;
    }

    public GetOffersResponse getOffers(String msisdn) {
        String transId = transIdGenerator.next();
        KongRequest request = new KongRequest(transId, msisdn,
                new KongRequest.ServiceData(props.lang(), props.channel()));
        log.info("get-offers transid={} msisdn={}", transId, mask(msisdn));

        KongResponse kongResponse;
        try {
            kongResponse = kongClient.getRecommendations(tokenProvider.getToken(), request);
        } catch (TokenRejectedException e) {
            log.warn("transid={} token rejected by Kong, refreshing and retrying once", transId);
            tokenProvider.invalidate();
            kongResponse = kongClient.getRecommendations(tokenProvider.getToken(), request);
        }

        GetOffersResponse response = mapper.toResponse(kongResponse);
        log.info("get-offers transid={} result={} offers={}", transId, response.status(), response.data().size());
        return response;
    }

    private static String mask(String msisdn) {
        return msisdn.length() <= 4 ? "****" : "*".repeat(msisdn.length() - 4) + msisdn.substring(msisdn.length() - 4);
    }
}
