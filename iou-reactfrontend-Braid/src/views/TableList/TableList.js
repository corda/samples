import React, {
  Component
} from 'react';
// @material-ui/core components
import {
  makeStyles
} from "@material-ui/core/styles";
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
import Select from '@material-ui/core/Select';
import Backdrop from '@material-ui/core/Backdrop';
import Fade from '@material-ui/core/Fade';
import styles from "assets/jss/material-dashboard-react/components/tableStyle.js";
import TextField from '@material-ui/core/TextField';
import MenuItem from '@material-ui/core/MenuItem';
import { withStyles } from '@material-ui/core/styles';


import {
  NetworkApi
} from 'code-gen/src';
import {
  ApiClient
} from 'code-gen/src';
import {
  CordappsApi
} from 'code-gen/src';
import { whiteColor } from 'assets/jss/material-dashboard-react';
import { Hidden } from '@material-ui/core';
export class TableList extends Component {
  constructor(props) {
      super(props);
      // Don't call this.setState() here!
      this.state = {
          transactions: [],
          contracts: [],
          open: false,
          value: 100,
          party: "Counter Party",
          NetworkParties: []
      };
      this.handleOpen = this.handleOpen.bind(this);
      this.handleClose = this.handleClose.bind(this);
      this.proposalIOUflow = this.proposalIOUflow.bind(this);
      this.updateInput = this.updateInput.bind(this);
      this.selectParty = this.selectParty.bind(this);

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
      selectEmpty: {
        marginTop: theme.spacing(2),
        color: whiteColor
      },
  }));
  

  componentDidUpdate() {}
  componentDidMount() {
    console.log("Component Mounting")
    const cordappsApi = new CordappsApi(new ApiClient());
    cordappsApi.cordappsBootcampOpenapiFlowsBootcampGetAllTokensFlow({}, (error, data, response) => {
      console.log(data)
      this.setState({
        transactions: data
      });
    });
    console.log("Pulling network Participants: ")
    const netapi = new NetworkApi(new ApiClient());
    netapi.networkNodes({}, (error, data, response) => {
      this.setState({
        NetworkParties: data
      });
    });
  }
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

  proposalIOUflow() {
      var that = this;
      console.log("Propose IOU");
      const cordappsApi = new CordappsApi(new ApiClient());
      const netapi = new NetworkApi(new ApiClient());
      console.log("Pre-propose")
      console.log(this.state.party)

      netapi.networkNodes({'x500Name': this.state.party}, (error, data, response) => {
        console.log("Data: ")  
        console.log(JSON.stringify(data[0].legalIdentities[0].name));
          var tokenIssueInput = JSON.stringify({
              "owner": {
                  "name": data[0].legalIdentities[0].name,
                  "owningKey": data[0].legalIdentities[0].owningKey
              },
              "amount": this.state.value
          });
          console.log(tokenIssueInput)
          cordappsApi.cordappsBootcampOpenapiFlowsBootcampTokenIssueFlowInitiator(tokenIssueInput, (error, data, response) => {
            console.log("Inside Token Issue")  
            console.log(data)
              cordappsApi.cordappsBootcampOpenapiFlowsBootcampGetAllTokensFlow({}, (error, data, response) => {
                console.log("Updating the list")  
                console.log(data)
                  this.setState({
                      transactions: data
                  });
              });
          });
      })
  }
  
  updateInput(event) {
      this.setState({
          value: event.target.value
      })
      console.log(this.state.value);
  }

  selectParty(event){
    this.setState({
      party:event.target.value
    })
    console.log("Selected party: ")
    console.log(this.state.party)
  }


  render(){
    const StyledTableRow = withStyles(theme => ({
      root: {
        'border-bottom': 'Hidden',
      },
    }))(TableRow);
    return( 
    <GridContainer>
      <GridItem xs={ 10 } sm={ 8 } md={ 11 }>
        <Card>
          <CardHeader color="primary">
          <Table className={ this.useStyles.table }>
              <StyledTableRow className={ this.useStyles.tableHeadRow }>
                <TableCell align="left" ><h4 className={ this.useStyles.cardTitleWhite }> Transaction Table </h4></TableCell>
                <TableCell align="right" ><p className={ this.useStyles.cardCategoryWhite }>IOU Issuance Records from the node 's vault </p></TableCell>
              </StyledTableRow>
          </Table>
            <Table className={ this.useStyles.table }>
              <TableRow className={ this.useStyles.tableHeadRow }>
                <TableCell align="left" >Issue IOU to CounterParty</TableCell>
                  <TableCell align="right"> Select Counter Party: 
                  <Select value={this.state.party} color="primary" onChange={this.selectParty} displayEmpty className={this.useStyles.selectEmpty }>
                    <MenuItem value="" disabled>Counter Party</MenuItem>
                      {this.state.NetworkParties.map((party) => {
                        const name = party.legalIdentities[0].name
                        return(
                          <MenuItem value={name}>{name.substring(2,name.indexOf(","))}</MenuItem>
                        );})
                      }  
                  </Select>
                  </TableCell>
                  <TableCell align="right"> Enter Amount: 
                  <input type="text" onChange={this.updateInput}></input>
                  </TableCell>
                  <TableCell align="right">
                    <Button color="white" onClick = {this.proposalIOUflow}> Issue IOU State </Button>
                  </TableCell>
              </TableRow>
            </Table>
          </CardHeader> 
          <CardBody>
            <Table className={ this.useStyles.table }>
              <TableHead className={ "TableHeader" }>
                <TableRow className={ this.useStyles.tableHeadRow }>
                  <TableCell component="th" scope="row"> IOU Issuer </TableCell>
                  <TableCell  align="left" scope="row"> IOU Owner </TableCell>
                  <TableCell align="left"> Amount </TableCell>
                </TableRow>
              </TableHead>
              <TableBody> {this.state.transactions.map((item) => {
                const transaction = item.state.data
                return (
                  <TableRow className={ this.useStyles.tableBodyRow }>
                    <TableCell component="th" scope="row"> {transaction.issuer.name} </TableCell>
                    <TableCell  scope="row"> {transaction.owner.name} </TableCell>
                    <TableCell component="th" scope="row"> {transaction.amount} </TableCell>
                  </TableRow> 
                );})}
              </TableBody>
            </Table> 
          </CardBody> 
        </Card> 
      </GridItem> 
    </GridContainer>
    );
  }
}
export default TableList