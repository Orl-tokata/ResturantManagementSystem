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
using System.Data.SqlServerCe;

namespace ResturantManagement
{
    public partial class frmDashboard : Form
    {
        public frmDashboard()
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
        public double ExtractData(string sql)
        {
            ClassConnection.con.Open();
            SqlCeCommand cm = new SqlCeCommand(sql, ClassConnection.con);
            double data = double.Parse(cm.ExecuteScalar().ToString());
            ClassConnection.con.Close();
            return data;

        }
        private void frmHome_Load(object sender, EventArgs e)
        {
            string sdate = DateTime.Now.ToShortDateString();
            lblDalySale.Text = ExtractData("select COALESCE(sum(COALESCE(netAmt,0)),0) AS total from tblMain where status like 'Paid' and aDate between '" + sdate + "' and '" + sdate + "'").ToString("#,##0.00") + " $ " ;
            //lblTotalKH.Text = ExtractData("SELECT COALESCE(sum(COALESCE(totalKH,0)),0) AS total FROM tblMain WHERE status LIKE 'Paid' AND aDate BETWEEN '" + sdate + "' AND '" + sdate + "'").ToString("#,##0") + " ៛ ";
            lblTotalProduct.Text = ExtractData("select count(*) FROM tblMain where aDate between '" + sdate + "' and '" + sdate + "'").ToString("#,##0") + " Products ";
            lblStockOnHand.Text = ExtractData("select COALESCE(sum(COALESCE(qty,0)),0) AS qty from tblMain m inner join tblDetails d on d.MainID = m.MainID where m.aDate between '" + sdate + "' AND '" + sdate + "'").ToString("#,##0") + " Items ";
        }
    }
}
