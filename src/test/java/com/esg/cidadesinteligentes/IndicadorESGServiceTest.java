package com.esg.cidadesinteligentes;

import com.esg.cidadesinteligentes.model.IndicadorESG;
import com.esg.cidadesinteligentes.repository.IndicadorESGRepository;
import com.esg.cidadesinteligentes.service.IndicadorESGService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndicadorESGServiceTest {

    @Mock
    private IndicadorESGRepository repository;

    @InjectMocks
    private IndicadorESGService service;

    private IndicadorESG indicador;

    @BeforeEach
    void setUp() {
        indicador = IndicadorESG.builder()
                .id(1L)
                .cidade("São Paulo")
                .categoria("AMBIENTAL")
                .indicador("Emissão de CO2")
                .valor(120.5)
                .unidade("toneladas")
                .ano(2024)
                .build();
    }

    @Test
    @DisplayName("Deve listar todos os indicadores")
    void deveListarTodos() {
        when(repository.findAll()).thenReturn(List.of(indicador));
        var result = service.listarTodos();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCidade()).isEqualTo("São Paulo");
    }

    @Test
    @DisplayName("Deve buscar indicador por ID existente")
    void deveBuscarPorId() {
        when(repository.findById(1L)).thenReturn(Optional.of(indicador));
        var result = service.buscarPorId(1L);
        assertThat(result).isPresent();
        assertThat(result.get().getIndicador()).isEqualTo("Emissão de CO2");
    }

    @Test
    @DisplayName("Deve retornar vazio para ID inexistente")
    void deveRetornarVazioParaIdInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        var result = service.buscarPorId(99L);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve salvar novo indicador")
    void deveSalvarIndicador() {
        when(repository.save(indicador)).thenReturn(indicador);
        var result = service.salvar(indicador);
        assertThat(result.getId()).isEqualTo(1L);
        verify(repository, times(1)).save(indicador);
    }

    @Test
    @DisplayName("Deve deletar indicador por ID")
    void deveDeletarPorId() {
        doNothing().when(repository).deleteById(1L);
        service.deletar(1L);
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Deve buscar indicadores por cidade")
    void deveBuscarPorCidade() {
        when(repository.findByCidade("São Paulo")).thenReturn(List.of(indicador));
        var result = service.buscarPorCidade("São Paulo");
        assertThat(result).hasSize(1);
    }
}
