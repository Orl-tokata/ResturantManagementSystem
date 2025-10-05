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
using System.Data.SqlServerCe;

namespace ResturantManagement.View
{
    public partial class frmProductView : Form
    {
        public frmProductView()
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
        public double countProd(string sql)
        {
            ClassConnection.con.Open();
            SqlCeCommand cm = new SqlCeCommand(sql, ClassConnection.con);
            double data = double.Parse(cm.ExecuteScalar().ToString());
            ClassConnection.con.Close();
            return data;
        }
        private void frmProductView_Load(object sender, EventArgs e)
        {
            GetData();
        }

        public void GetData()
        {
            string qry = "select pID,pName,pPrice,CategoryID,c.catName from products p inner join category c on c.catID = p.CategoryID where pName like N'%" + txtSearch.Text + "%'";
            ListBox lb = new ListBox();
            lb.Items.Add(dgvid);
            lb.Items.Add(dgvName);
            lb.Items.Add(dgvPrice);
            lb.Items.Add(dgvcatID);
            lb.Items.Add(dgvcat);

            ClassConnection.LoadData(qry, guna2DataGridView1, lb);

            //TODO: count category
            lblTotalProd.Text = countProd("select count(*) from products").ToString();//+ " Products ";
        }
        private void btnAdd_Click(object sender, EventArgs e)
        {
            ClassConnection.BlueBackground(new Model.frmProductAdd());
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
                // it is change as we have to set form text propties befor open
                frmProductAdd frmAdd = new frmProductAdd();
                frmAdd.id = Convert.ToInt32(guna2DataGridView1.CurrentRow.Cells["dgvid"].Value);
                frmAdd.cID = Convert.ToInt32(guna2DataGridView1.CurrentRow.Cells["dgvcatID"].Value);

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
                    string qry = "Delete from products where pID = '" + id + "'";
                    Hashtable ht = new Hashtable();
                    ClassConnection.SQl(qry, ht);
                    //this.Alert("Deleted Successfull.", AlertMessage.enmType.Success);
                    showToast("SUCCESS", "Deleted Successfull.");
                    GetData();
                }
            }
        }
    }
}
