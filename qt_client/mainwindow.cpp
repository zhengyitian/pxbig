#include "mainwindow.h"

void MainWindow::iniUI()
{
    QVBoxLayout *v = new QVBoxLayout();
    QHBoxLayout *h = new QHBoxLayout();

    l1 = new myLine(QString::number(xStart));
    l2 = new myLine(QString::number(yStart));
    l3 = new myLine(QString::number(xLen));
    l4 = new myLine(QString::number(yLen));
    l5 = new myLine(QString::number(reso));
    connect(l1, SIGNAL(wh()), this, SLOT(onCli()));
    connect(l2, SIGNAL(wh()), this, SLOT(onCli()));
    connect(l3, SIGNAL(wh()), this, SLOT(onCli()));
    connect(l4, SIGNAL(wh()), this, SLOT(onCli()));
    connect(l5, SIGNAL(wh()), this, SLOT(onCli()));

    h->addWidget(l1);
    h->addWidget(l2);
    h->addWidget(l3);
    h->addWidget(l4);
    h->addWidget(l5);
    setBtn = new QPushButton("set");
    connect(setBtn, SIGNAL(clicked()), this, SLOT(onCli()));

    h->addWidget(setBtn);
    pauseBtn =  new QPushButton("||");
    connect(pauseBtn, SIGNAL(clicked()), this, SLOT(OnP()));
    h->addWidget(pauseBtn);
    v->addLayout(h);
    v->addStretch();
    this->setLayout(v);
    setGeometry(QRect(200,200,800,500));
}

bool MainWindow::eventFilter(QObject *obj, QEvent *e)
{
    if (e->type()==QEvent::KeyPress )
    {
        QKeyEvent *keyEvent = static_cast<QKeyEvent*>(e);
        qDebug()<<keyEvent->key();
        if(keyEvent->key()==16777220 ||keyEvent->key()==16777221)
        {
            onCli();
            return QWidget::eventFilter( obj, e );
        }
        if(keyEvent->key()==65)
            l1->setText(QString::number(l1->text().toInt()-10));

        else if(keyEvent->key()==74)
            l3->setText(QString::number(l3->text().toInt()-10));

        else if(keyEvent->key()==87)
            l2->setText(QString::number(l2->text().toInt()-10));

        else if(keyEvent->key()==73)
            l4->setText(QString::number(l4->text().toInt()-10));

        else if(keyEvent->key()==75)
            l4->setText(QString::number(l4->text().toInt()+10));

        else if(keyEvent->key()==76)
            l3->setText(QString::number(l3->text().toInt()+10));

        else if(keyEvent->key()==83)
            l2->setText(QString::number(l2->text().toInt()+10));

        else if(keyEvent->key()==81)
            l5->setText(QString::number(l5->text().toInt()-3));

        else if(keyEvent->key()==69)
            l5->setText(QString::number(l5->text().toInt()+3));

        else if(keyEvent->key()==68)
            l1->setText(QString::number(l1->text().toInt()+10));
 else if(keyEvent->key()==32)
    {
            OnP();
            e->accept();
            return true;
        }
        else
        {
            return QWidget::eventFilter( obj, e );
        }
        onCli();
        e->accept();
        return true;
    }
    return QWidget::eventFilter( obj, e );
}



void MainWindow::OnP()
{
    pause = !pause;
    if(pause)
        pauseBtn->setText("->");
    else {
        pauseBtn->setText("||");
    }
}

void MainWindow::setErr()
{
    if (tipNum==0)
        tipNum=1;
    else {
        tipNum=0;
    }
    setBtn->setText("err"+QString::number( tipNum));
}

void MainWindow::setOk()
{
    if (tipNum==0)
        tipNum=1;
    else {
        tipNum=0;
    }
    setBtn->setText("ok"+QString::number( tipNum));
}

void MainWindow::onCli()
{
    int be=reso;
    int   xS2 =  l1->text().toInt();
    int yS2 =  l2->text().toInt();
    int   xL2 =  l3->text().toInt();
    int  yL2 =  l4->text().toInt();
    reso =  l5->text().toInt();
    if (reso<0) reso=0;
    reso = int(reso/3)*3;
    if(be!=0 &&reso==0)
    {
        xStart = lastDrawInfo.oriXs;
        yStart = lastDrawInfo.oriYs;
        xLen = lastDrawInfo.oriXl;
        yLen = lastDrawInfo.oriYl;
        l1->setText (QString::number(xStart));
        l2 ->setText(QString::number(yStart));
        l3 ->setText(QString::number(xLen));
        l4->setText(QString::number(yLen));
        l5->setText(QString::number(reso));
        return  setOk();
    }
    if (xS2<0||yS2<0||xL2<=0||yL2<=0)
        return setErr();

    if (xS2+xL2>xCapLen||yS2+yL2>yCapLen)
        return setErr();
    xStart = xS2;yStart=yS2;xLen = xL2;yLen = yL2;
    setOk();
}
