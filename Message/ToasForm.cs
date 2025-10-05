using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ResturantManagement
{
    public partial class ToasForm : Form
    {
        int toastX, toastY;
        public ToasForm(string type, string message)
        {
            InitializeComponent();
            lblType.Text = type;
            lblMessage.Text = message;
            switch (type)
            {
                case "SUCCESS":
                    toasBorder.BackColor = Color.FromArgb(57, 155, 53);
                    picIcon.Image = Properties.Resources.mSuccess;
                    break;
                case "ERROR":
                    toasBorder.BackColor = Color.FromArgb(227, 50, 45);
                    picIcon.Image = Properties.Resources.mError;
                    break;
                case "INFO":
                    toasBorder.BackColor = Color.FromArgb(18, 136, 191);
                    picIcon.Image = ResturantManagement.Properties.Resources.mInfo;
                    break;
                case "WARNING":
                    toasBorder.BackColor = Color.FromArgb(245, 171, 35);
                    picIcon.Image = ResturantManagement.Properties.Resources.mWarning;
                    break;
            }
        }

        private void ToasForm_Load(object sender, EventArgs e)
        {
            Position();
        }

        private void toastTimer_Tick(object sender, EventArgs e)
        {
            toastY -= 10;
            this.Location = new Point(toastX, toastY);
            if(toastY <= 760)
            {
                toastTimer.Stop();
                toastHide.Start();
            }
        }
        int y = 100;
        private void toastHide_Tick(object sender, EventArgs e)
        {
            y--;
            if( y <= 0)
            {
                toastY += 1;
                this.Location = new Point(toastX, toastY += 10);
                if(toastY > 800)
                {
                    toastHide.Stop();
                    y = 100;
                    this.Close();
                }
            }
        }

        private void Position()
        {
            int ScreenWidth = Screen.PrimaryScreen.WorkingArea.Width;
            int ScreenHeight = Screen.PrimaryScreen.WorkingArea.Height;

            toastX = ScreenWidth - this.Width - 5;
            toastY = ScreenHeight - this.Height + 70;

            this.Location = new Point(toastX, toastY);
        }
    }
}
