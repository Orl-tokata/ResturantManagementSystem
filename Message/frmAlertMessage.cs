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
    public partial class frmAlertMessage : Form
    {
        public frmAlertMessage()
        {
            InitializeComponent();
        }
        public void Alert(string msg, AlertMessage.enmType type)
        {
            AlertMessage alert = new AlertMessage();
            alert.showAlert(msg, type);
        }
        public void showToast(string type, string message)
        {
            ToasForm toas = new ToasForm(type,message);
            toas.Show();
        }
        private void btnSuccess_Click(object sender, EventArgs e)
        {
            showToast("SUCCESS", "This is a Success");
            //this.Alert("Success Alert ",AlertMessage.enmType.Success);
        }

        private void btnError_Click(object sender, EventArgs e)
        {
            showToast("ERROR", "This is a Error");
            //this.Alert("Error Alert ", AlertMessage.enmType.Error);
        }

        private void btnWarning_Click(object sender, EventArgs e)
        {
            showToast("WARNING", "This is a Warning");
            //this.Alert("Warning Alert ", AlertMessage.enmType.Warning);
        }

        private void btnInfo_Click(object sender, EventArgs e)
        {
            showToast("INFO", "This is a Information");
            //this.Alert("Info Alert ", AlertMessage.enmType.Info);
        }
    }
}
