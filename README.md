# ATMAS
Air Traffic Multi Agent System

Agentes: Avião, Aeroporto

Aeroporto: canal de comunicação onde se encontram todos os aviões que o têm como destino e os que se encontram nele.
Avião:
- Tem uma probabilidade pequena de declarar emergência durante o voo, tendo prioridade de aterragem sobre os outros.

Ciclo normal de um avião:
1. Descolagem
- Escolhe uma hora desejada
- Pergunta a todos pelos seus horários de chegada e descolagem
- Escolhe a hora livre mais próxima da desejada
- Comunica a todos a hora escolhida
- Descola
- Informa aeroporto que se irá desconetar naquele momento
- Desconeta do aeroporto
2. Viagem
- 2.1 Conetar ao aeroporto de destino
- 2.2 Recebe todos os aviões deste
- 2.3 Comunicar com todos para saber quando pode aterrar
- 2.4 Se não houver horário livre, pede aos outros que mudem eles as suas horas, começando pelo que tem a hora mais próxima deste (pesquisa em largura)
- Ajustamento da velocidade baseada em quando pode aterrar
- Escolhe a hora de chegada
3. Aterragem
- Aterra
4. Tempo de inatividade
- Rinse and repeat

Avião em emergência (declarada durante o voo):
- Coneta-se ao aeroporto mais próximo, desligando-se do anterior se for outro
- Comunicar aos outros a emergência, tendo prioridade sobre os que não estão em emergência para aterrar

Cenários extremos:
- Falta de combustível extra para adiar a aterragem
- Avião A descola, inicia viagem e calcula sua hora de aterragem. Depois, avião B descola, inicia viagem e se for à velocidade máxima, só tem combustível para chegar à hora que o avião A escolheu.
- executa a partir de 2.3

 
Premissas:
- Combustível/tempo = constante
- Um avião no final do estado de aterragem nunca pode abortar
- Todos os aviões sabem sempre a localização e como conetar a todos os aeroportos
- Não há falhas nem atrasos de comunicação
- Entre aterragens e descolagens tem de haver pelo menos 3 minutos de intervalo
- Não há filas a entrar e sair da pista: taxiways têm capacidade ilimitada
- Só há 1 estado de emergência, não podem tomar prioridades entre eles

# AgenteAviao {
- combustível_max;
- velocidade_max;
- velocidade_min: 0.7 da max;
- velocidade;
- combustível_atual;
- taxa-combustivel(combustivel/s);
- posiçao;
- estado: {voo, aterrar, parado, acidente};
- emergência: bool;
- lista-de-aeroportos[];
- aeroporto_destino;
- aeroporto_partida;
- hora_prevista_de_chegada:
- hora_máxima_de_aterragem;
- hora_mínima_de_aterragem;
}

# AgenteAeroporto {
- posição:
- numero-pistas: //maybe
- lista-com-todos-os-avioes-conectados.
}
