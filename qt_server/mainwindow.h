#ifndef MAINWINDOW_H
#define MAINWINDOW_H


#include <QMainWindow>


#include "qdebug.h"
#include "qdatetime.h"
#include "qscreen.h"
#include "QtNetwork/qtcpsocket.h"
#include "qtcpserver.h"
#include "qguiapplication.h"
#include "zlib.h"
#include "QtEndian"

class MainWindow : public QWidget
{
    Q_OBJECT
public:

    QTcpServer* server = new QTcpServer(this);
    QTcpSocket *socket;
    QByteArray data;

    explicit MainWindow(QWidget *parent = nullptr)
    {

        server->listen(QHostAddress::Any, 8899);
        connect(server, SIGNAL(newConnection()), SLOT(newConnection()));
    }

public slots:
    void newConnection()
    {
        static int co=0;
        co++;
        socket = server->nextPendingConnection();
        socket->waitForReadyRead(10000);
        auto a = socket->readAll();
        // qDebug()<<co<<"read:"<<a.size()<<QDateTime::currentMSecsSinceEpoch();
        if(a.size()!=16)
        {
            socket->deleteLater();
            socket = nullptr;
            return;
        }

        int xs,ys,xl,yl;
        memcpy((char*)&xs,a.data(),4);
        memcpy((char*)&ys,a.data()+4,4);
        memcpy((char*)&xl,a.data()+8,4);
        memcpy((char*)&yl,a.data()+12,4);
        QScreen *screen  =  QGuiApplication::primaryScreen();
        auto im=screen->grabWindow(0,xs, ys, xl, yl).toImage();
        QByteArray d;
        for(int j=0;j<yl;j++)
        {
            for(int i=0;i<xl;i++)
            {
                auto argb = im.pixel(i,j);
                d.push_back(qRed(argb));
                d.push_back(qGreen(argb));
                d.push_back(qBlue(argb));
                d.push_back(88);
            }
        }
        auto t1 = QDateTime::currentMSecsSinceEpoch();
        auto re = qCompress(d);
        qDebug()<<d.size()<<re.size()<<QDateTime::currentMSecsSinceEpoch()-t1;

        if (re.size()<d.size())
        {
            socket->write("\x01",1);
            quint32 ll = re.size()-4;
            unsigned char cc[4];
            qToBigEndian(ll,cc);
            socket->write((const char*)cc,4);
            socket->write(re.data()+4,re.size()-4);
        }

        else {
            socket->write("\x00",1);
            socket->write(d.data(),d.size());
        }



        //   auto t1 = QDateTime::currentMSecsSinceEpoch();
        while (QDateTime::currentMSecsSinceEpoch()-t1<10*1000) {
            socket->waitForBytesWritten(1000);
            auto ww =  socket->bytesToWrite();
            //  qDebug()<<co<<"write:"<<ww<<QDateTime::currentMSecsSinceEpoch();
            if (ww==0)
                break;
        }
        socket->close();
        socket->deleteLater();
        socket = nullptr;
    }
};

#endif
