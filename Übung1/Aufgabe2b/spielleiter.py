import socket
import threading
import time
import argparse

class Spielleiter:
    def __init__(self, host, port, spieler_latenz, dauer_der_runde, anzahl_der_runden):
        self.host = host
        self.port = port
        self.spieler_latenz = spieler_latenz
        self.dauer_der_runde = dauer_der_runde
        self.anzahl_der_runden = anzahl_der_runden
        self.clients = []
        self.results = []
        self.runde = 0
        self.logical_clock = 0
        self.running = True

    def increment_clock(self):
        self.logical_clock += 1

    def start_server(self):
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.bind((self.host, self.port))
        server_socket.listen()
        print(f'Spielleiter wartet auf Verbindungen bei {self.host}:{self.port}')

        threading.Thread(target=self.accept_clients, args=(server_socket,)).start()

    def accept_clients(self, server_socket):
        end_time = time.time() + ANMELDEZEIT
        while self.running and time.time() < end_time:
            try:
                server_socket.settimeout(end_time - time.time())
                client_socket, addr = server_socket.accept()
                self.clients.append(client_socket)
                threading.Thread(target=self.handle_client, args=(client_socket,)).start()
                print(f'Spieler verbunden von {addr}')
            except socket.timeout:
                break

    def start_round(self):
        print(f'Warte {ANMELDEZEIT} Sekunden, damit sich Spieler anmelden können...')
        time.sleep(ANMELDEZEIT)
        print('Anmeldezeit beendet. Spiel startet.')

        while self.runde < self.anzahl_der_runden and self.running:
            self.runde += 1
            self.increment_clock()
            start_message = f'START:{self.logical_clock}'
            print('Runde beginnt')
            for client in self.clients:
                client.sendall(start_message.encode('utf-8'))

            time.sleep(self.dauer_der_runde)

            self.increment_clock()
            stop_message = f'STOP:{self.logical_clock}'
            for client in self.clients:
                client.sendall(stop_message.encode('utf-8'))

            self.evaluate_round()

        print("Spiel beendet")
        self.shutdown_server()

    def evaluate_round(self):
        highest_roll = 0
        winner = None
        all_rolls = []

        for result in self.results:
            name, roll, logical_time = result
            all_rolls.append((name, roll, logical_time))
            if roll > highest_roll:
                highest_roll = roll
                winner = name

        with open('results.txt', 'a') as f:
            f.write(f'Runde {self.runde}:\n')
            for name, roll, logical_time in all_rolls:
                winner_mark = ' <- Gewinner' if name == winner else ''
                f.write(f'Logische Uhr {logical_time} - {name} würfelt {roll}{winner_mark}\n')
            f.write('\n')

        self.results.clear()

    def handle_client(self, client_socket):
        while self.running:
            data = client_socket.recv(1024).decode('utf-8')
            if data:
                name, roll, client_clock = data.split(':')
                roll = int(roll)
                client_clock = int(client_clock)
                self.increment_clock()
                self.logical_clock = max(self.logical_clock, client_clock)
                self.results.append((name, roll, self.logical_clock))

    def shutdown_server(self):
        for client in self.clients:
            client.close()
        print("Server heruntergefahren")

    def stop(self):
        self.running = False

def stop_server(spielleiter):
    while True:
        command = input()
        if command == 'q':
            spielleiter.stop()
            break

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Spielleiter für das Würfelspiel')
    parser.add_argument('--host', type=str, default='0.0.0.0', help='Host-Adresse des Servers')
    parser.add_argument('--port', type=int, default=12345, help='Portnummer des Servers')
    parser.add_argument('--spieler_latenz', type=int, default=5, help='Maximale Latenzzeit für Spieler in Sekunden')
    parser.add_argument('--dauer_der_runde', type=int, default=10, help='Dauer jeder Runde in Sekunden')
    parser.add_argument('--anzahl_der_runden', type=int, default=5, help='Anzahl der Runden im Spiel')
    args = parser.parse_args()

    ANMELDEZEIT = 130  # Zeit zum Anmelden der Spieler vor der ersten Runde

    spielleiter = Spielleiter(args.host, args.port, args.spieler_latenz, args.dauer_der_runde, args.anzahl_der_runden)
    threading.Thread(target=spielleiter.start_server).start()
    threading.Thread(target=stop_server, args=(spielleiter,)).start()
    spielleiter.start_round()
