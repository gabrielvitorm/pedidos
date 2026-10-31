package br.com.pedidos.api.domain.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DominioSemFrameworksTest {

    @Test
    void modelosDoDominioSaoRecordsSemSettersOuAnotacoesDeFramework() {
        var modelos = Stream.of(Pedido.class, ItemPedido.class, SolicitacaoPagamento.class);

        modelos.forEach(modelo -> {
            assertThat(modelo.isRecord()).isTrue();
            assertThat(Arrays.stream(modelo.getAnnotations()))
                    .allMatch(annotation -> !annotation.annotationType().getPackageName().startsWith("org.springframework"));
            assertThat(Arrays.stream(modelo.getDeclaredMethods()))
                    .noneMatch(this::eSetter);
        });
    }

    private boolean eSetter(Method method) {
        return method.getName().startsWith("set")
                && method.getParameterCount() == 1
                && Modifier.isPublic(method.getModifiers());
    }
}
