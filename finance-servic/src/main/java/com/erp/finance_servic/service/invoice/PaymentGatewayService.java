package com.erp.finance_servic.service.invoice;

import com.erp.finance_servic.dto.invoice.request.PaymentGatewayRequest;
import com.erp.finance_servic.dto.invoice.response.PaymentGatewayResponse;
import com.erp.finance_servic.entity.invoice.PaymentGatewayEntity;
import com.erp.finance_servic.repository.invoice.PaymentGatewayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

    private final PaymentGatewayRepository paymentGatewayRepository;

    public PaymentGatewayResponse createPaymentGateway(PaymentGatewayRequest request) {
        PaymentGatewayEntity gateway = PaymentGatewayEntity.builder()
                .name(request.getName())
                .build();

        gateway = paymentGatewayRepository.save(gateway);
        return mapToPaymentGatewayResponse(gateway);
    }

    public List<PaymentGatewayResponse> getAllPaymentGateways() {
        return paymentGatewayRepository.findAll().stream()
                .map(this::mapToPaymentGatewayResponse)
                .collect(Collectors.toList());
    }

    public void deletePaymentGateway(Long id) {
        paymentGatewayRepository.deleteById(id);
    }

    private PaymentGatewayResponse mapToPaymentGatewayResponse(PaymentGatewayEntity gateway) {
        return PaymentGatewayResponse.builder()
                .id(gateway.getId())
                .name(gateway.getName())
                .createdAt(gateway.getCreatedAt())
                .build();
    }
}
