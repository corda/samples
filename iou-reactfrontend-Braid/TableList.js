import React, {Component} from 'react';
// @material-ui/core components
import { makeStyles} from "@material-ui/core/styles";
// core components
import GridItem from "components/Grid/GridItem.js";
import GridContainer from "components/Grid/GridContainer.js";
import Table from "@material-ui/core/Table";
import Card from "components/Card/Card.js";
import CardHeader from "components/Card/CardHeader.js";
import CardBody from "components/Card/CardBody.js";
import Grid from '@material-ui/core/Grid';
import Modal from '@material-ui/core/Modal';
import Input from '@material-ui/core/Input';
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import Button from "components/CustomButtons/Button.js";
import Backdrop from '@material-ui/core/Backdrop';
import Fade from '@material-ui/core/Fade';
import styles from "assets/jss/material-dashboard-react/components/tableStyle.js";
import TextField from '@material-ui/core/TextField';
import { NetworkApi } from 'code-gen/src';
import { ApiClient } from 'code-gen/src';
import { CordappsApi } from 'code-gen/src';
export class TableList extends Component {
  constructor(props) {
    super(props);
    // Don't call this.setState() here!
    this.state = {
      transactions: [],
      contracts: [],
      open: false,
      value:100
    };
    this.handleOpen = this.handleOpen.bind(this);
    this.handleClose = this.handleClose.bind(this);
    this.sendsettleflow = this.sendsettleflow.bind(this);
    this.proposalIOUflow = this.proposalIOUflow.bind(this);
    this.selfIssueflow = this.selfIssueflow.bind(this);
    this.updateInput = this.updateInput.bind(this);
  }
  useStyles = makeStyles(theme => ({
  paper: {
    position: 'absolute',
    width: 400,
    backgroundColor: theme.palette.background.paper,
    border: '2px solid #000',
    boxShadow: theme.shadows[5],
    padding: theme.spacing(2, 4, 3),
  },
}));
  handleOpen() {
    console.log("setting true");
    this.setState({
      open: true
    })
  }
  handleClose() {
    this.setState({
      open: false
    })
  }
  sendsettleflow(amount,lender) {
    const am = amount.substring(0,amount.indexOf(" "));
    const cur = amount.substring(amount.indexOf(" ")+1).str;
    var url = new URL("http://localhost:10009/api/iou/issue-iou?amount=7799&currency=USD&party=C=FR,L=Paris,O=ParticipantC");
    // url.searchParams.set('amount', 7799);
    // url.searchParams.set('currency', USD);
    // url.searchParams.set('party', C=FR,L=Paris,O=ParticipantC);
    fetch('http://localhost:10009/api/iou/issue-iou?amount=9999&currency=USD&party=C=FR,L=Paris,O=ParticipantC', {
            method: 'PUT',
            mode: 'CORS',
            body: {}
        }).then(res => {
            console.log(res);
        }).catch(err => err)
  }
  proposalIOUflow() {
    var that = this;
    console.log("Propose IOU");
    const cordappsApi = new CordappsApi(new ApiClient());
   const netapi = new NetworkApi(new ApiClient());
    netapi.networkNodes({'x500-name': 'O=PartyB,L=New York,C=US'}, (error, data, response) => {
      console.log(JSON.stringify(data[0].legalIdentities[0].owningKey));
      var tokenIssueInput = JSON.stringify({"owner": {"name": data[0].legalIdentities[0].name,"owningKey": data[0].legalIdentities[0].owningKey},"amount": this.state.value});
      console.log(tokenIssueInput)
      cordappsApi.cordappsBootcampOpenapiFlowsBootcampTokenIssueFlowInitiator(tokenIssueInput, (error, data, response) => {
      //
      console.log(data)
cordappsApi.cordappsBootcampOpenapiFlowsBootcampGetAllTokensFlow({},(error, data, response) => {
      console.log(data)
      this.setState({
      transactions:data
      });
      });
      });
                                                                                                                                                                                })
  }
  selfIssueflow() {
    console.log("Self issue money");
  }
  componentDidUpdate(){
  }
  componentDidMount() {
  const cordappsApi = new CordappsApi(new ApiClient());
//getter    cordappsBootcampOpenapiFlowsBootcampGetAllTokensFlow
cordappsApi.cordappsBootcampOpenapiFlowsBootcampGetAllTokensFlow({},(error, data, response) => {
      console.log(data)
      this.setState({
      transactions:data
      });
      });
  }
  updateInput(event){
     this.setState({value : event.target.value})
     console.log(this.state.value);
   }
  render() {
    return ( < GridContainer>
      < GridItem xs={ 10 } sm={ 8 } md={ 11 }>
      < Card>
      <CardHeader color="primary">
      < h4 className={ this.useStyles.cardTitleWhite }> Transaction Table < /h4>
      < p className={ this.useStyles.cardCategoryWhite }>Token Issuance Records from the node 's vault < /p> 
      <Table className={ this.useStyles.table }>
        <TableRow style={ { float: 'right' } } className={ this.useStyles.tableHeadRow }>
         
          <TableCell > Issue Tokens to CounterParty <input type="text" onChange={this.updateInput}></input>< Button  color="white" 　onClick = {this.proposalIOUflow}> Issue Token </Button></TableCell>

        </TableRow>
      </Table>
      </CardHeader> 
      < CardBody>
      
      < Table className={ this.useStyles.table }>
      < TableHead className={ "TableHeader" }>
        < TableRow className={ this.useStyles.tableHeadRow }>
          < TableCell component="th" scope="row"> Issuer < /TableCell>
                    < TableCell  align="left" scope="row"> Owner < /TableCell>
          < TableCell align="left"> Amount < /TableCell>
        < /TableRow>
      < /TableHead>





      
      < TableBody> {this.state.transactions.map((item) => {

        const transaction = item.state.data
          return (
            < TableRow className={ this.useStyles.tableBodyRow }>
            < TableCell component="th" scope="row"> {transaction.issuer.name} < /TableCell>
            < TableCell  scope="row"> {transaction.owner.name} < /TableCell>
                        < TableCell component="th" scope="row"> {transaction.amount} < /TableCell>
              < /TableRow> ); }) }
              < /TableBody> < /Table> < /CardBody> < /Card> < /GridItem> < /GridContainer> ); } }
              export default TableList