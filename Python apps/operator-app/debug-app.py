#!/usr/bin/python3
# -*- coding: utf-8 -*-

import sys
from PyQt5.QtWidgets import (QApplication, QWidget, QPushButton, QGridLayout, QComboBox,
 QVBoxLayout, QHBoxLayout, QMessageBox, QDesktopWidget, QLineEdit, QLabel)
import json
import requests


class Example(QWidget):
    
    def __init__(self):
        super().__init__()
        
        self.initUI()
        
    def initUI(self):        
        self.setGeometry(300, 300, 300, 220)
        self.setWindowTitle('Operator app')
        self.center()    

        mainLayout    = QGridLayout(self)

        self.ipEdit    = QLineEdit('http://195.208.117.228:8080')
        ipTitle   = QLabel('IP:')
        mainLayout.addWidget(ipTitle      , 0, 0)
        mainLayout.addWidget(self.ipEdit  , 0, 1, 1, 5)

        self.uuids = QComboBox()
        uuidTitle = QLabel('UUID:')
        createActorbtn = QPushButton('Create', self)
        deleteActorbtn = QPushButton('Delete', self)
        mainLayout.addWidget(uuidTitle    , 1, 0)
        mainLayout.addWidget(self.uuids, 1, 1, 1, 3)
        mainLayout.addWidget(createActorbtn, 1, 4)
        mainLayout.addWidget(deleteActorbtn, 1, 5)
        createActorbtn.clicked.connect(self.createActor)

        frwdBtn   = QPushButton('Forward', self)
        leftBtn   = QPushButton('Left', self)
        bkwdBtn   = QPushButton('Backward', self)
        rightBtn  = QPushButton('Right', self)
        mainLayout.addWidget(frwdBtn, 3, 2)
        mainLayout.addWidget(leftBtn, 4, 1)
        mainLayout.addWidget(bkwdBtn, 4, 2)
        mainLayout.addWidget(rightBtn, 4, 3)
        frwdBtn.clicked.connect(self.buttonClicked)
        leftBtn.clicked.connect(self.buttonClicked)
        bkwdBtn.clicked.connect(self.buttonClicked)
        rightBtn.clicked.connect(self.buttonClicked)

        self.show()

    def createActor(self):
        data = {'actorType': 'ParrotActor'}
        self.headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
        req = requests.put(self.ipEdit.text() + '/actor', data=json.dumps(data), headers=self.headers)
        if req.json()['Status'] == 'Success':
            task_id = req.json()['TaskId']
        else:
            raise Exception("""Can't register task""")
        resp = self.wait_for_task(task_id)
        if resp.json()['Status'] == "Success":
            [client_id, sub_string, send_string] = \
                [resp.json()['clientUID'], resp.json()['subString'], resp.json()['sendString']]
        else:
            raise Exception("""Can't execute task with reason: """ + resp.json()['Result'])
        self.uuids.addItem(client_id)

    def wait_for_task(self, task_id):
        status = 'Incomplete'
        while status == 'Incomplete':
            url = self.ipEdit.text() + '/task'
            data = {'Id': task_id}
            req = requests.get(url, data=json.dumps(data), headers=self.headers)
            status = req.json()['Status']
        if req.json()['Status'] == 'Error':
            raise Exception("""Error on task with id: """ + task_id + " with reason: " + req.json()['Result'])
        return req

    def sendCommand(self, command):
        data = {'clientUID': self.uuids.currentText(), 'command': command, 'args':[]}
        headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
        req = requests.post(self.ipEdit.text() + '/command', data=json.dumps(data), headers=headers)

    def buttonClicked(self):
        sender = self.sender()
        self.sendCommand(sender.text())

    def closeEvent(self, event):        
        reply = QMessageBox.question(self, 'Message',
            "Are you sure to quit?", QMessageBox.Yes | 
            QMessageBox.No, QMessageBox.No)
        if reply == QMessageBox.Yes:
            event.accept()
        else:
            event.ignore() 

    def center(self):        
        qr = self.frameGeometry()
        cp = QDesktopWidget().availableGeometry().center()
        qr.moveCenter(cp)
        self.move(qr.topLeft())
        
        
if __name__ == '__main__':    
    app = QApplication(sys.argv)
    ex = Example()
    sys.exit(app.exec_())  