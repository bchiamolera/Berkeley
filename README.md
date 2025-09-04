## Grupo:
- Alana Andreazza
- Bernardo Chiamolera
- Leticia Fruet
- Thierry Zancanaro

# Passos do funcionamento de Berkeley:
1. Servidor solicita a hora dos clientes.
2. Cada cliente responde ao servidor informando qual é a diferença de tempo em relação a ele.
3. O servidor efetua a média dos tempos (incluindo a leitura dele).
4. O servidor encaminha o ajuste necessário a ser feito pelo cliente (média + inversão da diferença de tempo enviada no passo 2).
5. Cliente realiza o ajuste.
