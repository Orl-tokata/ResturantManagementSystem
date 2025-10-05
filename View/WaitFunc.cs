using Microsoft.Office.Interop.Excel;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ResturantManagement.View
{
    internal class WaitFunc
    {
        loading load;
        Thread loadthread;


        public void Show()
        {
            // loadthread
            loadthread = new Thread(new ThreadStart(LoadingProcess));
        }
        public void Show(Form parent)
        {
            loadthread = new Thread(new ParameterizedThreadStart(LoadingProcess));
            loadthread.Start(parent);
        }
        public void Close()
        {
            if(load != null)
            {
                load.BeginInvoke (new System.Threading.ThreadStart(load.CloseLoading));
                load = null;
                loadthread = null;
            }
        }

        private void LoadingProcess()
        {
            load = new loading();
            load.ShowDialog();
        }

       private void LoadingProcess(object parent)
        {
            Form parent1 = parent as Form;
            load = new loading(parent1);
            load.ShowDialog();
        }
    }
}
