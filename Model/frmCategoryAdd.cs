using Guna.UI2.WinForms;
using ResturantManagement.View;
using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.Linq;
using System.Security.Policy;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Data.SqlServerCe;

namespace ResturantManagement.Model
{
    public partial class frmCategoryAdd : Form
    {
        public frmCategoryAdd()
        {
            InitializeComponent();
        }
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

        public int id = 0;

        private void btnSave_Click(object sender, EventArgs e)
        {
            string qry = "";
            if (txtName.Text != "")
            {
                if (id == 0) // insert
                {
                    qry = "insert into category (catName) values(@catName) ";
                }
                else // update
                {
                    qry = "update category set catName = @catName where catID = @id ";
                }
                ClassConnection.con.Open();
                SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
                cmd.Parameters.AddWithValue("@id", id);
                cmd.Parameters.Add("@catName", SqlDbType.NVarChar, 50);
                cmd.Parameters["@catName"].Value = txtName.Text;
                //cmd.Parameters.AddWithValue("@catName", txtName.Text);
                ClassConnection.con.Close();
                if (ClassConnection.con.State == ConnectionState.Closed) { ClassConnection.con.Open(); }
                if (id > 0)
                {
                    id = Convert.ToInt32(cmd.ExecuteScalar());
                    //this.Alert("Saved Successfully.", AlertMessage.enmType.Success);
                    showToast("SUCCESS", "Saved Successfully.");
                    txtName.Focus();
                    txtName.Clear();
                    ClassConnection.con.Close();
                }
                else
                {
                    id = Convert.ToInt32(cmd.ExecuteScalar());
                    //this.Alert("Saved Successfully.", AlertMessage.enmType.Success);
                    showToast("SUCCESS", "Saved Successfully.");
                    txtName.Focus();
                    txtName.Clear();
                    ClassConnection.con.Close();
                }
            }
            else
            {
                //this.Alert("Please input category name!", AlertMessage.enmType.Warning);
                showToast("WARNING", "Please input category name!");
            }
        }
        private void btnClose_Click(object sender, EventArgs e)
        {
            this.Close();
        }
    }
}
