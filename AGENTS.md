# Regras do projeto

## Objetivo

Este serviço é uma API Spring Boot para o domínio de pedidos. O código deve evoluir com foco em clareza, regras de negócio explícitas, baixo acoplamento e facilidade de teste.

## Stack e comando
- Use Java 21 e Spring Boot 4.1.1.
- Execute Maven sempre pelo Maven Wrapper compatível com o sistema operacional:
  - Windows: `.\mvnw.cmd`
  - Linux/macOS: `./mvnw`
- Antes de afirmar que algo está funcionando, execute os testes e mostre a evidência do comando.
- Não instale ferramentas, altere versões ou adicione dependências sem pedir autorização.

## Arquitetura

- Organize o serviço segundo arquitetura hexagonal (ports and adapters).
- O domínio contém entidades, objetos de valor e regras de negócio; não deve depender de Spring, JPA ou detalhes de infraestrutura.
- A camada de aplicação coordena casos de uso e também não deve depender de Spring ou JPA. Dependências externas devem ser representadas por portas.
- Adaptadores de entrada e saída podem conhecer frameworks e infraestrutura, mas não devem mover regras de negócio para fora do domínio.
- Preserve limites claros entre domínio, aplicação e adaptadores.

## Regras de implementação

- Represente valores monetários com `BigDecimal`; não use `double` ou `float` para dinheiro.
- Não use Lombok.
- Não adicione nenhuma dependência nova sem pedir autorização previamente.
- Não altere configuração, banco, Docker ou infraestrutura sem que isso esteja explicitamente no escopo aprovado.

## Fluxo obrigatório para mudanças

1. Inspecione o projeto e leia a spec indicada pelo usuário antes de propor uma implementação.
2. Faça um plano curto, com arquivos envolvidos, comportamento esperado e verificação.
3. Aguarde o OK explícito do usuário antes de editar.
4. Implemente somente o escopo aprovado.
5. Execute os testes pelo Maven Wrapper e registre o resultado.
6. Mostre o diff relevante e informe qualquer limitação ou bloqueio.

Estas regras são a fonte principal para agentes que trabalham neste projeto. `CLAUDE.md` deve apenas encaminhar o Claude Code para este arquivo.
