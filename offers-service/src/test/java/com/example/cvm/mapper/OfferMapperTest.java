package com.example.cvm.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.cvm.dto.GetOffersResponse;
import com.example.cvm.dto.KongResponse;
import com.example.cvm.dto.KongResponse.Offers;
import com.example.cvm.dto.KongResponse.Status;
import com.example.cvm.dto.OfferDto;
import com.fasterxml.jackson.databind.ObjectMapper;

class OfferMapperTest {

    private final OfferMapper mapper = new OfferMapper();
    private final ObjectMapper json = new ObjectMapper();

    private KongResponse sample() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/kong-response.json")) {
            return json.readValue(in, KongResponse.class);
        }
    }

    @Test
    void mapsSampleKongResponse() throws Exception {
        GetOffersResponse res = mapper.toResponse(sample());

        assertThat(res.status()).isTrue();
        assertThat(res.code()).isEqualTo(1);
        assertThat(res.message()).isEqualTo("Success");
        assertThat(res.data()).hasSize(2);

        OfferDto first = res.data().get(0);
        assertThat(first.offerId()).isEqualTo("234589222");
        assertThat(first.offerShortDesc()).isEqualTo("10GB 28hari 40rb");
        assertThat(first.registrationKey()).isEqualTo("RCRP1MB30D50K");
        assertThat(first.validity()).isEqualTo("30");
        assertThat(first.description()).startsWith("100% Kuota Utama|");
        assertThat(first.retailerIncentive()).isEqualTo("2000");
        assertThat(first.starred()).isEqualTo("true");
        assertThat(first.bestDeal()).isEqualTo("true");
        assertThat(first.cashbackValue()).isEqualTo("0");
        assertThat(first.order()).isEqualTo(1);
        assertThat(first.price()).isEqualTo(38000L);

        OfferDto second = res.data().get(1);
        assertThat(second.offerId()).isEqualTo("234589223");
        assertThat(second.registrationKey()).isEqualTo("CVMBNSXGB28D");
        assertThat(second.price()).isEqualTo(46500L);
    }

    @Test
    void sortsByOrder() throws Exception {
        KongResponse kong = sample();
        List<KongResponse.KongOffer> reversed = new ArrayList<>(kong.offers().rechargeOffer());
        Collections.reverse(reversed);

        GetOffersResponse res = mapper.toResponse(new KongResponse(kong.status(), new Offers(reversed)));

        assertThat(res.data()).extracting(OfferDto::order).containsExactly(1, 2);
    }

    @Test
    void kongErrorStatusReturnsFailure() {
        GetOffersResponse res = mapper.toResponse(new KongResponse(new Status(500, "Boom"), null));

        assertThat(res.status()).isFalse();
        assertThat(res.code()).isZero();
        assertThat(res.message()).isEqualTo("Boom");
        assertThat(res.data()).isEmpty();
    }

    @Test
    void emptyOffersReturnsFailure() {
        GetOffersResponse res = mapper.toResponse(
                new KongResponse(new Status(200, "Success"), new Offers(List.of())));

        assertThat(res.status()).isFalse();
        assertThat(res.message()).isEqualTo("No offers available");
    }
}
