package com.restosuite.service;

import com.restosuite.common.ApiException;
import com.restosuite.domain.DocumentSequence;
import com.restosuite.repo.DocumentSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SequenceService {

    private final DocumentSequenceRepository sequences;

    public SequenceService(DocumentSequenceRepository sequences) {
        this.sequences = sequences;
    }

    @Transactional
    public String next(Long branchId, String docType) {
        DocumentSequence sequence = sequences.lock(branchId, docType)
                .orElseThrow(() -> ApiException.badRequest(
                        "No hay una numeración configurada para " + docType + " en esta sucursal. Defínala en Configuración."));
        long current = sequence.getNextNumber();
        sequence.setNextNumber(current + 1);
        return sequence.getPrefix() + String.format("%0" + sequence.getPadding() + "d", current);
    }
}
