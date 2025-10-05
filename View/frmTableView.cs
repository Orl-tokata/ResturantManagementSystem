using ResturantManagement.Model;
using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlServerCe;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ResturantManagement.View
{
    public partial class frmTableView : Form
    {
        public frmTableView()
        {
            InitializeComponent();
        }
        // this Enable double buffering for all the controls
        protected override CreateParams CreateParams
        {
            get
            {
                CreateParams handleParam = base.CreateParams;
                handleParam.ExStyle |= 0x02000000;
                return handleParam;
            }
        }
        public void showToast(string type, string message)
        {
            ToasForm toas = new ToasForm(type, message);
            toas.Show();
        }

        public double countTable(string sql)
        {
            ClassConnection.con.Open();
            SqlCeCommand cm = new SqlCeCommand(sql, ClassConnection.con);
            double data = double.Parse(cm.ExecuteScalar().ToString());
            ClassConnection.con.Close();
            return data;
        }
        private void frmTableView_Load(object sender, EventArgs e)
        {
            GetData();
        }

        public void GetData()
        {
            string qry = "select tid, tname ,status from tables where tname like '%" + txtSearch.Text + "%'";
            ListBox lb = new ListBox();
            lb.Items.Add(dgvid);
            lb.Items.Add(dgvName);
            lb.Items.Add(dgvStatus);
            ClassConnection.LoadData(qry, guna2DataGridView1, lb);

            //TODO: count Table
            lblTotalTable.Text = countTable("select count(*) from tables").ToString();//+ " Table ";
        }

        private void btnAdd_Click(object sender, EventArgs e)
        {
            ClassConnection.BlueBackground(new Model.frmTableAdd());
            GetData();
        }
        private void txtSearch_TextChanged_1(object sender, EventArgs e)
        {
            GetData();
        }

        private void guna2DataGridView1_CellContentClick(object sender, DataGridViewCellEventArgs e)
        {
            if (guna2DataGridView1.CurrentCell.OwningColumn.Name == "dgvedit")
            {
                frmTableAdd frmAdd = new frmTableAdd();
                frmAdd.id = Convert.ToInt32(guna2DataGridView1.CurrentRow.Cells["dgvid"].Value);
                frmAdd.txtName.Text = Convert.ToString(guna2DataGridView1.CurrentRow.Cells["dgvName"].Value);
                frmAdd.cbStatus.Text = Convert.ToString(guna2DataGridView1.CurrentRow.Cells["dgvStatus"].Value);
                ClassConnection.BlueBackground(frmAdd);
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
                    string qry = "Delete from tables where tID = '" + id + "'";
                    Hashtable ht = new Hashtable();
                    ClassConnection.SQl(qry, ht);

                    guna2MessageDialog1.Icon = Guna.UI2.WinForms.MessageDialogIcon.Question;
                    guna2MessageDialog1.Buttons = Guna.UI2.WinForms.MessageDialogButtons.YesNo;
                    //this.Alert("Deleted Successfull.", AlertMessage.enmType.Success);
                    showToast("SUCCESS", "Deleted Successfull.");
                    GetData();
                }
            }
        }
    }
}
