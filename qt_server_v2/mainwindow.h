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
#include <QTime>
#include <QMap>
#include <QMutex>
#include <QString>
#include "QHBoxLayout"
#include "qpushbutton.h"
#include "qlineedit.h"
#include "qlabel.h"
#include "QTimer"

void delay_noblock( int millisecondsToWait );

class MainWindow : public QWidget
{
    Q_OBJECT
public:
    QMutex mutex;
    QMap<QString,QByteArray> record_map;
    QMap<QString,qint64> lastsend_time;
    QTcpServer* server = new QTcpServer(this);
    int timeout_limit = 8000;
    int gap = 200;
    int x_gap = 0;
    int y_gap = 0;
    QLineEdit* l1;
    QLineEdit* l2;
    QLineEdit* l3;
    QLineEdit* l4;
    QLineEdit* l5;
    int sendco = 0;

    explicit MainWindow(QWidget *parent = nullptr)
    {

        QTimer *timer = new QTimer(this);
        connect(timer, SIGNAL(timeout()), this,SLOT(ontimer()) );
        timer->start(1000);

        server->listen(QHostAddress::Any, 8899);
        connect(server, SIGNAL(newConnection()), SLOT(newConnection()));

        QVBoxLayout *v = new QVBoxLayout();
        QHBoxLayout *h = new QHBoxLayout();
        QHBoxLayout *h2 = new QHBoxLayout();
        auto la1 = new QLabel("x_gap");
        l1 = new QLineEdit("0");
        h->addWidget(la1);
        h->addWidget(l1);

        auto la2 = new QLabel("y_gap");
        l2 = new QLineEdit("0");
        h->addWidget(la2);
        h->addWidget(l2);

        auto la3 = new QLabel("interval(ms)");
        l3 = new QLineEdit("200");
        h2->addWidget(la3);
        h2->addWidget(l3);

        auto la4 = new QLabel("timeout(ms)");
        l4 = new QLineEdit(QString::number( timeout_limit));
        h2->addWidget(la4);
        h2->addWidget(l4);

        auto qb = new QPushButton("set");
        connect(qb, SIGNAL(clicked()), this, SLOT(onCli()));
        v->addLayout(h);
        v->addLayout(h2);
        v->addWidget(qb);
        l5 = new QLineEdit("0");
        v->addWidget(l5);
        this->setLayout(v);

    }

    void get_pic( int xs,int ys,int xl,int yl,QByteArray &re)
    {
        QScreen *screen  =  QGuiApplication::primaryScreen();
        auto im=screen->grabWindow(0,xs+x_gap, ys+y_gap, xl, yl).toImage();
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
        re = qCompress(d);
        //   qDebug()<<d.size()<<re.size()<<QDateTime::currentMSecsSinceEpoch()-t1;
    }

public slots:
    void ontimer()
    {
        l5->setText(QString::number(sendco));
    }
    void onCli()
    {
        x_gap =  l1->text().toInt();
        y_gap =  l2->text().toInt();
        gap =  l3->text().toInt();
        timeout_limit =  l4->text().toInt();

    }
    void newConnection()
    {
        static int co=0;
        co++;
        QTcpSocket *   socket = server->nextPendingConnection();
        socket->waitForReadyRead(10000);
        auto a = socket->readAll();
        qDebug()<<co<<"read:"<<a.size()<<QDateTime::currentMSecsSinceEpoch();
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

        QString tt = socket->peerAddress().toString()+"_"+ QString::number(xs)+
                "_"+ QString::number(ys)+
                "_"+ QString::number(xl)+
                "_"+ QString::number(yl);
        qDebug()<<tt;
        mutex.lock();
        if(!record_map.contains(tt))
        {
            record_map[tt] = QByteArray();
            lastsend_time[tt] = 0;
        }
        mutex.unlock();
        QByteArray re;
        int waitco = 0;
        int sleepms = 0;

        while (1) {
            get_pic( xs,ys,xl,yl,re);
            mutex.lock();
            if (record_map[tt]!=re)
            {
                qDebug()<<"get change";
                sendco++;
                record_map[tt] = re;
                auto tnow = QDateTime::currentMSecsSinceEpoch();

                if(tnow-lastsend_time[tt]<gap)
                {
                    sleepms = gap+lastsend_time[tt]-tnow;
                    lastsend_time[tt] = lastsend_time[tt]+gap;
                }
                else{
                    lastsend_time[tt]  = tnow;
                }
                mutex.unlock();
                break;
            }
            mutex.unlock();
                delay_noblock(10);
            socket->waitForReadyRead(100);
            waitco ++;
            if(waitco>timeout_limit/100)
            {
                qDebug()<<"timeout";
                socket->close();
                socket->deleteLater();
                socket = nullptr;
                return;
            }
        }
        qDebug()<<"sleepms"<<sleepms;

        if(sleepms>0)
        {
            socket->waitForReadyRead(sleepms);
             delay_noblock(10);
        }


        socket->write("\x01",1);
        quint32 ll = re.size()-4;
        unsigned char cc[4];
        qToBigEndian(ll,cc);
        socket->write((const char*)cc,4);
        socket->write(re.data()+4,re.size()-4);

        auto t1 = QDateTime::currentMSecsSinceEpoch();
        while (QDateTime::currentMSecsSinceEpoch()-t1<10*1000) {
            socket->waitForBytesWritten(1000);
            auto ww =  socket->bytesToWrite();
            if (ww==0)
                break;
        }
        qDebug()<<"over";
        socket->close();
        socket->deleteLater();
        socket = nullptr;
    }
};

#endif
