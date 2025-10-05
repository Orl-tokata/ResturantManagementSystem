using ResturantManagement.Model;
using System;
using System.Collections;
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
    public partial class frmStaffView : Form
    {
        public frmStaffView()
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
        private void frmStaffView_Load(object sender, EventArgs e)
        {
            GetData();
        }

        public void GetData()
        {
            string qry = "select * from staff where sName like '%" + txtSearch.Text + "%'";
            ListBox lb = new ListBox();
            lb.Items.Add(dgvid);
            lb.Items.Add(dgvName);
            lb.Items.Add(dgvPhone);
            lb.Items.Add(dgvRole);

            MainClass.LoadData(qry, guna2DataGridView1, lb);
        }

        private void btnAdd_Click_1(object sender, EventArgs e)
        {
            MainClass.BlueBackground(new Model.frmStaffAdd());
            GetData();
        }

        private void txtSearch_TextChanged(object sender, EventArgs e)
        {
            GetData();
        }

        private void guna2DataGridView1_CellContentClick(object sender, DataGridViewCellEventArgs e)
        {
            if (guna2DataGridView1.CurrentCell.OwningColumn.Name == "dgvedit")
            {
                // it is change as we have to set form text propties befor open
                frmStaffAdd frmAdd = new frmStaffAdd();
                frmAdd.id = Convert.ToInt32(guna2DataGridView1.CurrentRow.Cells["dgvid"].Value);
                frmAdd.txtName.Text = Convert.ToString(guna2DataGridView1.CurrentRow.Cells["dgvName"].Value);
                frmAdd.txtPhone.Text = Convert.ToString(guna2DataGridView1.CurrentRow.Cells["dgvPhone"].Value);
                frmAdd.cbRole.Text = Convert.ToString(guna2DataGridView1.CurrentRow.Cells["dgvRole"].Value);
                MainClass.BlueBackground(frmAdd);
                GetData();

            }

            // need to confirm befor delete data
            if (guna2DataGridView1.CurrentCell.OwningColumn.Name == "dgvdel")
            {
                guna2MessageDialog1.Icon = Guna.UI2.WinForms.MessageDialogIcon.Question;
                guna2MessageDialog1.Buttons = Guna.UI2.WinForms.MessageDialogButtons.YesNo;

                if (guna2MessageDialog1.Show("Are you sure want to delete?") == DialogResult.Yes)
                {
                    int id = Convert.ToInt32(guna2DataGridView1.CurrentRow.Cells["dgvid"].Value);
                    string qry = "Delete from staff where staffID = '" + id + "'";
                    Hashtable ht = new Hashtable();
                    MainClass.SQl(qry, ht);

                    guna2MessageDialog1.Icon = Guna.UI2.WinForms.MessageDialogIcon.Question;
                    guna2MessageDialog1.Buttons = Guna.UI2.WinForms.MessageDialogButtons.YesNo;
                    //guna2MessageDialog1.Show("Deleted Successfull");
                    // this.Alert("Deleted Successfull.", AlertMessage.enmType.Success);
                    showToast("SUCCESS", "Deleted Successfull.");
                    GetData();
                }
            }
        }
    }
}
