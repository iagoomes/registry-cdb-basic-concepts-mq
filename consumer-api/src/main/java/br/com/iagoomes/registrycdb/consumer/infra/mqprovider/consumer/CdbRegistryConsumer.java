package br.com.iagoomes.registrycdb.consumer.infra.mqprovider.consumer;

import br.com.iagoomes.registrycdb.consumer.domain.dto.CdbRegistryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CdbRegistryConsumer {

    @RabbitListener(queues = "${fixed-income.queue.name}")
    public void handleCdbRegistryCreated(CdbRegistryDto cdbRegistry) {
        log.info("Processing CDB registry: {}", cdbRegistry);
        log.info("Registry ID: {}, Client: {}, Amount: {}, Duration: {} days, Interest Rate: {}%",
                cdbRegistry.getRegistryId(),
                cdbRegistry.getClientId(),
                cdbRegistry.getAmount(),
                cdbRegistry.getDurationDays(),
                cdbRegistry.getInterestRate());
    }
}
