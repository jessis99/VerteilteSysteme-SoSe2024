import socket
import time
import random
import argparse

class Spieler:
    def __init__(self, name, host, port, max_latenz):
        self.name = name
        self.host = host
        self.port = port
        self.max_latenz = max_latenz
        self.logical_clock = 0
        self.running = True

    def increment_clock(self):
        self.logical_clock += 1

    def connect_to_server(self):
        client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client_socket.connect((self.host, self.port))
        print(f'Verbunden mit dem Server bei {self.host}:{self.port}')

        while self.running:
            data = client_socket.recv(1024).decode('utf-8')
            if data:
                message, server_clock = data.split(':')
                server_clock = int(server_clock)
                self.increment_clock()
                self.logical_clock = max(self.logical_clock, server_clock)

                if message == 'START':
                    self.play_round(client_socket)

        client_socket.close()

    def play_round(self, client_socket):
        latenzeit = random.uniform(0, self.max_latenz)
        time.sleep(latenzeit)
        self.increment_clock()
        wurf = random.randint(1, 100)
        print(f'{self.name} hat {wurf} geworfen')
        result_message = f'{self.name}:{wurf}:{self.logical_clock}'
        client_socket.sendall(result_message.encode('utf-8'))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Spieler für das Würfelspiel')
    parser.add_argument('--name', type=str, required=True, help='Name des Spielers')
    parser.add_argument('--host', type=str, required=True, help='Host-Adresse des Servers')
    parser.add_argument('--port', type=int, required=True, help='Portnummer des Servers')
    parser.add_argument('--max_latenz', type=int, default=5, help='Maximale Latenzzeit für Spieler in Sekunden')
    args = parser.parse_args()

    spieler = Spieler(args.name, args.host, args.port, args.max_latenz)
    spieler.connect_to_server()
