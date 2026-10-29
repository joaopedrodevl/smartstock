#!/usr/bin/env python3 -u
"""
Simulador de dispositivos ESP32 (Embarcados) para o SmartStock.

Uso:
  # Um dispositivo específico
  python3 simula-embarcado.py --chave-publica <UUID> --tipo VENDA

  # Todos os dispositivos do banco (concorrentes)
  python3 simula-embarcado.py --all

  # Com tags RFID reais (fallback se o banco não estiver acessível)
  python3 simula-embarcado.py --chave-publica <UUID> --tipo VENDA \
      --rfid 348FE310C752 --rfid F401794A8B6F

Por padrão, cada leitura escolhe aleatoriamente uma tag ATIVA do banco
(tags_rfid ligadas a produtos ativos).

Requer que o backend esteja rodando em http://localhost:8080
"""

import argparse
import asyncio
import json
import random
import signal
import sys
import time
from dataclasses import dataclass

import requests
import websockets

API_BASE = "http://localhost:8080/v1"
WS_URL = "ws://localhost:8080/ws/embarcado"

# Configuração do banco (mesma do smart/compose.yaml)
DB_CONTAINER = "smart-postgres-1"
DB_USER = "myuser"
DB_NAME = "mydatabase"
SQL_TAGS_ATIVAS = (
    "SELECT t.codigo_rfid FROM tags_rfid t "
    "JOIN produtos p ON t.produto_id = p.id "
    "WHERE t.ativo = true AND p.ativo = true"
)


def buscar_tags_ativas_banco() -> list[str]:
    """Busca no banco os códigos RFID ativos vinculados a produtos ativos."""
    import subprocess

    cmd = [
        "docker", "exec", DB_CONTAINER,
        "psql", "-U", DB_USER, "-d", DB_NAME,
        "-t", "-A", "-c", SQL_TAGS_ATIVAS,
    ]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=5)
        if result.returncode != 0:
            return []
        return [linha.strip() for linha in result.stdout.splitlines() if linha.strip()]
    except Exception as e:
        print(f"[DB] Erro ao consultar tags ativas: {e}")
        return []


@dataclass
class EmbarcadoConfig:
    chave_publica: str
    tipo: str
    descricao: str
    loja_id: str = ""


EMBARCADOS_PADRAO = [
    EmbarcadoConfig(
        chave_publica="2ed2a99c-d219-4d6f-8eec-c26bcac1920d",
        tipo="VENDA",
        descricao="PDV",
        loja_id="",
    ),
    EmbarcadoConfig(
        chave_publica="aa93c1a8-76c3-46a7-93d7-2200a15a89fe",
        tipo="CADASTRO",
        descricao="CDT",
        loja_id="",
    ),
]


def autenticar(chave_publica: str) -> str | None:
    """Autentica o dispositivo e retorna o token de sessão."""
    try:
        resp = requests.post(
            f"{API_BASE}/embarcado/auth",
            json={"chavePublica": chave_publica},
            timeout=5,
        )
        if resp.status_code == 200:
            data = resp.json()
            print(f"[AUTH] Token obtido. Expira em: {data['expiraEm']}")
            return data["tokenSessao"]
        else:
            print(f"[AUTH] Erro {resp.status_code}: {resp.text}")
            return None
    except requests.ConnectionError:
        print("[AUTH] Backend não está acessível em http://localhost:8080")
        return None


def gerar_codigo_rfid() -> str:
    """Gera um código RFID fake (12 caracteres hex)."""
    return ''.join(random.choices('0123456789ABCDEF', k=12))


def buscar_produto_com_rfid(token: str, loja_id: str) -> str | None:
    """Busca um produto com tag RFID vinculada. Retorna o codigoRfid ou None."""
    try:
        # Buscar produtos da loja (requer autenticação de admin)
        # Por enquanto, vamos retornar None para usar RFID fake
        # TODO: Implementar busca real quando houver endpoint público
        return None
    except Exception:
        return None


async def simular_device(chave_publica: str, tipo: str, tag: str, loja_id: str = "", rfids: list[str] | None = None):
    """Conecta ao WebSocket e simula o comportamento do ESP32."""

    print(f"\n{'='*50}")
    print(f"  Simulador SmartStock — {tipo} ({tag})")
    print(f"  Chave: {chave_publica}")
    if rfids:
        print(f"  Tags reais: {', '.join(rfids)}")
    print(f"{'='*50}\n")

    # Autenticar
    token = autenticar(chave_publica)
    if not token:
        print(f"[FAIL] [{tag}] Não foi possível autenticar.")
        return

    # Cache de RFID de produtos cadastrados
    rfid_cache = list(rfids or [])
    rfid_index = 0
    if not rfid_cache and loja_id and tipo == "VENDA":
        rfid = buscar_produto_com_rfid(token, loja_id)
        if rfid:
            rfid_cache.append(rfid)
            print(f"[RFID] [{tag}] Produto com RFID encontrado: {rfid}")
        else:
            print(f"[RFID] [{tag}] Nenhum produto com RFID encontrado na loja {loja_id}")

    # Conectar WebSocket
    try:
        async with websockets.connect(WS_URL) as ws:
            print(f"[WS] [{tag}] Conectado a {WS_URL}")

            # Registrar dispositivo
            registro = json.dumps({
                "tipo": "REGISTRO",
                "chavePublica": chave_publica,
            })
            await ws.send(registro)
            print(f"[WS] [{tag}] Registro enviado")

            # Aguardar confirmação
            response = await asyncio.wait_for(ws.recv(), timeout=5)
            data = json.loads(response)
            if data.get("tipo") == "REGISTRO_CONFIRMADO":
                print(f"[WS] [{tag}] Registro confirmado ✓")
            else:
                print(f"[WS] [{tag}] Resposta inesperada: {data}")

            print(f"\n[READY] [{tag}] Dispositivo pronto. Aguardando comandos...\n")

            # Loop principal: escutar mensagens
            async for message in ws:
                data = json.loads(message)
                tipo_msg = data.get("tipo")

                if tipo_msg == "SOLICITAR_LEITURA":
                    leitura_id = data.get("leituraId")
                    print(f"[RFID] [{tag}] Solicitação de leitura recebida — ID: {leitura_id}")

                    # Simular delay de leitura (0.5 a 2 segundos)
                    delay = random.uniform(0.5, 2.0)
                    print(f"[RFID] [{tag}] Simulando leitura... ({delay:.1f}s)")
                    await asyncio.sleep(delay)

                    # Prioridade: tag aleatória do banco > tags via --rfid > tag fake
                    codigo_rfid = None
                    tags_banco = buscar_tags_ativas_banco()
                    if tags_banco:
                        codigo_rfid = random.choice(tags_banco)
                        print(f"[RFID] [{tag}] Tag lida (aleatória do banco): {codigo_rfid}")
                    elif rfid_cache:
                        codigo_rfid = rfid_cache[rfid_index % len(rfid_cache)]
                        rfid_index += 1
                        print(f"[RFID] [{tag}] Tag lida (--rfid): {codigo_rfid}")
                    else:
                        codigo_rfid = gerar_codigo_rfid()
                        print(f"[RFID] [{tag}] Tag lida (fake): {codigo_rfid}")

                    # Enviar leitura
                    rfid_msg = json.dumps({
                        "tipo": "RFID_LIDO",
                        "leituraId": leitura_id,
                        "codigoRfid": codigo_rfid,
                    })
                    await ws.send(rfid_msg)
                    print(f"[RFID] [{tag}] Leitura enviada ✓\n")

                elif tipo_msg == "REGISTRO_CONFIRMADO":
                    print(f"[WS] [{tag}] Re-registro confirmado")

                else:
                    print(f"[WS] [{tag}] Mensagem recebida: {json.dumps(data, indent=2)}")

    except websockets.exceptions.ConnectionClosed:
        print(f"[WS] [{tag}] Conexão fechada pelo servidor")
    except asyncio.TimeoutError:
        print(f"[WS] [{tag}] Timeout aguardando resposta")
    except Exception as e:
        print(f"[ERROR] [{tag}] {e}")


async def run_all(loja_id: str = "", rfids: list[str] | None = None):
    """Executa todos os embarcados concorrentemente."""
    tasks = [
        asyncio.create_task(
            run_with_reconnect(e.chave_publica, e.tipo, e.descricao, loja_id or e.loja_id, rfids)
        )
        for e in EMBARCADOS_PADRAO
    ]
    await asyncio.gather(*tasks)


async def run_with_reconnect(chave_publica: str, tipo: str, tag: str, loja_id: str = "", rfids: list[str] | None = None):
    """Executa o dispositivo com reconexão automática (útil quando o backend reinicia)."""
    while True:
        try:
            await simular_device(chave_publica, tipo, tag, loja_id, rfids)
        except Exception as e:
            print(f"[ERROR] [{tag}] {e}")
        print(f"[RECONNECT] [{tag}] Reconectando em 3 segundos...")
        await asyncio.sleep(3)


def main():
    parser = argparse.ArgumentParser(
        description="Simulador de dispositivos ESP32 SmartStock",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Exemplos:
  %(prog)s --chave-publica 550e8400-e29b-41d4-a716-446655440000 --tipo VENDA
  %(prog)s --all
        """,
    )
    parser.add_argument(
        "--chave-publica", "-k",
        help="Chave pública (UUID) do dispositivo cadastrado no sistema",
    )
    parser.add_argument(
        "--tipo", "-t",
        choices=["VENDA", "CADASTRO"],
        help="Tipo do dispositivo: VENDA ou CADASTRO",
    )
    parser.add_argument(
        "--all", "-a",
        action="store_true",
        help="Executar todos os embarcados do banco concorrentemente",
    )
    parser.add_argument(
        "--loja-id", "-l",
        help="UUID da loja para buscar produtos com RFID (obrigatório para tipo VENDA)",
    )
    parser.add_argument(
        "--rfid", "-r",
        action="append",
        default=[],
        help="Código RFID de fallback caso o banco não esteja acessível. "
             "Pode ser repetido para alternar entre várias tags.",
    )

    args = parser.parse_args()

    if not args.all and (not args.chave_publica or not args.tipo):
        parser.error("Informe --chave-publica e --tipo, ou use --all")

    # Tratar Ctrl+C
    def signal_handler(sig, frame):
        print("\n\n[EXIT] Encerrando simulador...")
        sys.exit(0)

    signal.signal(signal.SIGINT, signal_handler)

    if args.all:
        asyncio.run(run_all(args.loja_id or "", args.rfid or None))
    else:
        asyncio.run(
            run_with_reconnect(args.chave_publica, args.tipo, "MANUAL", args.loja_id or "", args.rfid or None)
        )


if __name__ == "__main__":
    main()
