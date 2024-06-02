import socket
import time
import random
import threading

class Spieler:
    def __init__(self, host, port, name):
        self.host = host
        self.port = port
        self.name = name
        self.running = True

    def connect_to_server(self):
        self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.client_socket.connect((self.host, self.port))
        print(f'{self.name} verbunden mit {self.host}:{self.port}')
        self.listen_for_messages()

    def listen_for_messages(self):
        while self.running:
            data = self.client_socket.recv(1024).decode('utf-8')
            if data == 'START':
                self.play_round()
            elif data == 'STOP':
                print(f'{self.name} beendet Runde')

    def play_round(self):
        SPIELER_LATENZ = 5;
        delay = random.uniform(0, SPIELER_LATENZ)
        time.sleep(delay)
        roll = random.randint(1, 100)
        message = f'{self.name}:{roll}'
        self.client_socket.sendall(message.encode('utf-8'))
        print(f'{self.name} würfelt {roll} nach {delay:.2f} Sekunden')

    def stop(self):
        self.running = False
        self.client_socket.close()
        print(f'{self.name} hat die Verbindung geschlossen')

if __name__ == '__main__':
    host = '172.20.10.3'  # IP address of the Spielleiter (Server)
    port = 12345  # Same port number as the Spielleiter
    name = input("Geben Sie Ihren Spielernamen ein: ")
    spieler = Spieler(host, port, name)
    threading.Thread(target=spieler.connect_to_server).start()
