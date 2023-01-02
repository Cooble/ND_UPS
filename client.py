import argparse
import asyncio
import tkinter as tk

REMOTE_ADDRESS = ('127.0.0.1', 1231)

parser = argparse.ArgumentParser()
parser.add_argument('-a', '--address', help='ip address and port', type=str)
args = parser.parse_args()

if args.address is not None:
    REMOTE_ADDRESS = (args.address.split(":")[0],int(args.address.split(":")[1]))

print("Address "+REMOTE_ADDRESS[0]+":"+str(REMOTE_ADDRESS[1]))

from tkinter.simpledialog import askstring




def global_callBack(s):
    print("Sending transport "+s)
    transport.sendto(s.encode())



class EchoClientProtocol:
    def __init__(self, loop):
        self.loop = loop
        self.transport = None

    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        print("recevied message")
        app.receive_message(data.decode())

    def error_received(self, exc):
        app.receive_message('Error received:'+str(exc))

    def connection_lost(self, exc):
        print("Socket closed, stop the event loop")
        loop = asyncio.get_event_loop()
        loop.stop()



from tkinter import *


class ChatApp:
    def __init__(self, master):
        self.master = master
        self.create_widgets()

    def create_widgets(self):
        # create a frame for the received messages
        received_frame = tk.Frame(self.master)
        received_frame.pack(side='left', fill='both', expand=True)
        # create a scrollbar for the received messages
        received_scrollbar = tk.Scrollbar(received_frame)
        received_scrollbar.pack(side='right', fill='y')
        # create a listbox for the received messages
        self.received_listbox = tk.Listbox(received_frame, yscrollcommand=received_scrollbar.set)
        self.received_listbox.pack(side='left', fill='both', expand=True)
        received_scrollbar.config(command=self.received_listbox.yview)

        # create a frame for the sent messages
        sent_frame = tk.Frame(self.master)
        sent_frame.pack(side='right', fill='both', expand=True)

        # create a frame for the message input
        input_frame = tk.Frame(sent_frame)
        input_frame.pack(side='bottom', fill='x')
        # create an entry for the message input
        self.message_input = tk.Entry(input_frame)
        self.message_input.pack(side='left', fill='x', expand=True)
        # create a "Send" button
        send_button = tk.Button(input_frame, text='Send', command=self.send_message)
        send_button.pack(side='right')

        # create a scrollbar for the sent messages
        sent_scrollbar = tk.Scrollbar(sent_frame)
        sent_scrollbar.pack(side='right', fill='y')
        # create a listbox for the sent messages
        self.sent_listbox = tk.Listbox(sent_frame, yscrollcommand=sent_scrollbar.set)
        self.sent_listbox.pack(side='left', fill='both', expand=True)
        sent_scrollbar.config(command=self.sent_listbox.yview)

    def send_message(self):
        message = self.message_input.get()
        self.sent_listbox.insert('end', message)
        global_callBack(message)
        #self.message_input.delete(0, 'end')

    def receive_message(self,message):
        self.received_listbox.insert('end', message)


# GUI
root = Tk()
root.title(REMOTE_ADDRESS[0]+":"+str(REMOTE_ADDRESS[1]))
app = ChatApp(root)


async def tk_main(roo):
    while True:
        roo.update()
        await asyncio.sleep(0.05)

tkmain = asyncio.ensure_future(tk_main(root))


loop = asyncio.get_event_loop()


connect = loop.create_datagram_endpoint(
    lambda: EchoClientProtocol(loop),
    remote_addr=REMOTE_ADDRESS)
transport, protocol = loop.run_until_complete(connect)
try:
    loop.run_forever()
except KeyboardInterrupt:
    pass
tkmain.cancel()
# See PyCharm help at https://www.jetbrains.com/help/pycharm/
