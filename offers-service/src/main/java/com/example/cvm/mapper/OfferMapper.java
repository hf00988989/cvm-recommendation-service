package com.example.cvm.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.cvm.dto.GetOffersResponse;
import com.example.cvm.dto.KongResponse;
import com.example.cvm.dto.KongResponse.KongOffer;
import com.example.cvm.dto.OfferDto;

@Component
public class OfferMapper {

    private static final int KONG_OK = 200;

    public GetOffersResponse toResponse(KongResponse kong) {
        if (kong == null || kong.status() == null) {
            return GetOffersResponse.failure("Invalid response from Kong");
        }
        Integer code = kong.status().code();
        if (code == null || code != KONG_OK) {
            String msg = kong.status().message() != null ? kong.status().message() : "Kong returned an error";
            return GetOffersResponse.failure(msg);
        }
        List<KongOffer> offers = kong.offers() == null ? null : kong.offers().rechargeOffer();
        if (offers == null || offers.isEmpty()) {
            return GetOffersResponse.failure("No offers available");
        }
        List<OfferDto> data = offers.stream()
                .map(this::toOffer)
                .sorted(Comparator.comparing(OfferDto::order, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return GetOffersResponse.success(data);
    }

    private OfferDto toOffer(KongOffer o) {
        Map<String, List<String>> p = o.additionalParams() == null ? Map.of() : o.additionalParams();
        return new OfferDto(
                o.id(),
                o.shortMessage(),
                first(p, "registrationKey"),
                first(p, "validity"),
                first(p, "Description"),
                first(p, "retailerIncentive"),
                first(p, "starred"),
                first(p, "bestDeal"),
                first(p, "cashbackValue"),
                o.order(),
                o.price());
    }

    private static String first(Map<String, List<String>> params, String key) {
        List<String> values = params.get(key);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}
