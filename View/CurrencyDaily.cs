using Guna.UI2.WinForms;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Xml.Linq;
using System.Data.SqlServerCe;

namespace ResturantManagement.View
{
    public partial class CurrencyDaily : Form
    {
        public CurrencyDaily()
        {
            InitializeComponent();
        }
        // validate input number only
        public int id = 0;
        /*public void Alert(string msg, AlertMessage.enmType type)
        {
            AlertMessage alert = new AlertMessage();
            alert.showAlert(msg, type);
        }*/
        public void showToast(string type, string message)
        {
            ToasForm toas = new ToasForm(type, message);
            toas.Show();
        }
        private void btnClose_Click(object sender, EventArgs e)
        {
            this.Close();
        }

        private void btnSave_Click(object sender, EventArgs e)
        {
            string currency = txtCurrency.Text;
            if (txtCurrency.Text == "")
            {
                //this.Alert("Please input currency.", AlertMessage.enmType.Error);
                showToast("ERROR", "Please input currency.");
            }
            else
            {
                ClassConnection.con.Open();
                string qry = "update tblCurrency set cuChange ='" + txtCurrency.Text + "'";
                SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);

                cm.ExecuteNonQuery();
                ClassConnection.con.Close();

                txtCurrency.Clear();
                //this.Alert("Currency Insert Successfully.", AlertMessage.enmType.Success);
                showToast("SUCCESS", "Currency Insert Successfully.");
            }
        }
        private void txtCurrency_KeyPress(object sender, KeyPressEventArgs e)
        {
            e.Handled = !char.IsDigit(e.KeyChar) && !char.IsControl(e.KeyChar) && e.KeyChar != '.' && e.KeyChar != ',';
        }
    }
}
