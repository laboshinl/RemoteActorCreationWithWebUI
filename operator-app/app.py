#!/usr/bin/python3
# -*- coding: utf-8 -*-

import sys
from PyQt5.QtWidgets import (QApplication, QWidget, QPushButton,
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

        buttonsLayout = QVBoxLayout(self)
        ipLayout      = QHBoxLayout(self)
        uuidLayout    = QHBoxLayout(self)

        self.ipEdit    = QLineEdit('http://127.0.0.1:8080')
        ipTitle   = QLabel('IP:')
        ipLayout.addWidget(ipTitle)
        ipLayout.addWidget(self.ipEdit)
        self.uuidEdit  = QLineEdit()
        uuidTitle = QLabel('UUID:')
        uuidLayout.addWidget(uuidTitle)
        uuidLayout.addWidget(self.uuidEdit)

        frwdBtn   = QPushButton('Forward', self)
        leftBtn   = QPushButton('Left', self)
        bkwdBtn   = QPushButton('Backward', self)
        rightBtn  = QPushButton('Right', self)
        buttonsLayout.addLayout(ipLayout)
        buttonsLayout.addLayout(uuidLayout)
        buttonsLayout.addWidget(frwdBtn)
        buttonsLayout.addWidget(leftBtn)
        buttonsLayout.addWidget(bkwdBtn)
        buttonsLayout.addWidget(rightBtn)
        frwdBtn.clicked.connect(self.buttonClicked)
        leftBtn.clicked.connect(self.buttonClicked)
        bkwdBtn.clicked.connect(self.buttonClicked)
        rightBtn.clicked.connect(self.buttonClicked)

        self.show()

    def sendCommand(self, command):
        data = {'clientUID': self.uuidEdit.text(), 'command': command, 'args':[]}
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