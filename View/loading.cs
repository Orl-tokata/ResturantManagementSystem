using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ResturantManagement.View
{
    public partial class loading : Form
    {
        public loading()
        {
            InitializeComponent();
            this.StartPosition = FormStartPosition.CenterParent;
        }
        public loading(Form paren)
        {
            InitializeComponent();
            if(paren != null)
            {
                this.StartPosition = FormStartPosition.Manual;
                this.Location = new Point(paren.Location.X + paren.Width / 2 - this.Width / 2,
                    paren.Location.Y + paren.Height / 2 - this.Height / 2);
            }
            else
            {
                this.StartPosition = FormStartPosition.CenterParent;
            }
        }
        public void CloseLoading()
        {
            this.DialogResult = DialogResult.OK;
            //this.Close();
            timer1.Start();
            if (lblLoading.Image != null)
            {
                lblLoading.Image.Dispose();
            }
        }
        private void timer1_Tick(object sender, EventArgs e)
        {
            if (this.Opacity > 0.0)
            {
                this.Opacity -= 0.075;
            }
            else
            {
                timer1.Stop();
                this.Close();
            }
        }
    }
}
