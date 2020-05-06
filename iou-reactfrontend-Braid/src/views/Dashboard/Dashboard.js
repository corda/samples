import React, { Component } from 'react';
// react plugin for creating charts
import PropTypes from "prop-types";
// react plugin for creating charts
import ChartistGraph from "react-chartist";
// @material-ui/core
import { makeStyles } from "@material-ui/core/styles";
import Icon from "@material-ui/core/Icon";
// @material-ui/icons
import Store from "@material-ui/icons/Store";
import Warning from "@material-ui/icons/Warning";
import DateRange from "@material-ui/icons/DateRange";
import LocalOffer from "@material-ui/icons/LocalOffer";
import Update from "@material-ui/icons/Update";
import ArrowUpward from "@material-ui/icons/ArrowUpward";
import AccessTime from "@material-ui/icons/AccessTime";
import Accessibility from "@material-ui/icons/Accessibility";
import BugReport from "@material-ui/icons/BugReport";
import Code from "@material-ui/icons/Code";
import Cloud from "@material-ui/icons/Cloud";
// core components
import GridItem from "components/Grid/GridItem.js";
import GridContainer from "components/Grid/GridContainer.js";
import Table from "@material-ui/core/Table";
import Tasks from "components/Tasks/Tasks.js";
import CustomTabs from "components/CustomTabs/CustomTabs.js";
import Danger from "components/Typography/Danger.js";
import Card from "components/Card/Card.js";
import CardHeader from "components/Card/CardHeader.js";
import CardIcon from "components/Card/CardIcon.js";
import CardBody from "components/Card/CardBody.js";
import CardFooter from "components/Card/CardFooter.js";
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import Button from "components/CustomButtons/Button.js";



import {
  dailySalesChart,
  emailsSubscriptionChart,
  completedTasksChart
} from "variables/charts.js";

import styles from "assets/jss/material-dashboard-react/views/dashboardStyle.js";
import { NetworkApi } from 'code-gen/src';
import { ApiClient } from 'code-gen/src';

const useStyles = makeStyles(styles);

export class Dashboard extends Component {
  constructor(props) {
    super(props);
    // Don't call this.setState() here!
    this.state = {NetworkParties: []};
  }
  componentDidMount() {
    const netapi = new NetworkApi(new ApiClient());
    netapi.networkNodes({}, (error, data, response) => {
      this.setState({
        NetworkParties: data
      });
    });
  }


  componentDidUpdate() {
  }




  render(){
    return (
      <div>
      <Table className={useStyles.table}>
      <TableHead className="TableHeader">
      <TableRow className={useStyles.tableHeadRow}>
      <TableCell component="th" scope="row">Counter Party</TableCell>
      <TableCell align="right">City</TableCell>
      <TableCell align="right">Country</TableCell>
      <TableCell align="right">X500Name</TableCell>
      </TableRow>
      </TableHead>
      <TableBody>
      {this.state.NetworkParties.map((party) => {
        const name = party.legalIdentities[0].name
        return(
          <TableRow className={useStyles.tableBodyRow}>
          <TableCell component="th" scope="row" >{name.substring(2,name.indexOf(","))}</TableCell>
          <TableCell align="right" >{name.substring(12, name.indexOf(",", name.indexOf(",") + 1))}</TableCell>
          <TableCell align="right" >{name.substring(name.indexOf(",", name.indexOf(",") + 2)+4)}</TableCell>
          <TableCell align="right" >{name}</TableCell>
          </TableRow>
        );})}
        </TableBody>
        </Table>
        </div>
      );
    }
  }
  Dashboard.defaultProps = {
    tableHeaderColor: "gray"
  };

  Dashboard.propTypes = {
    tableHeaderColor: PropTypes.oneOf([
      "warning",
      "primary",
      "danger",
      "success",
      "info",
      "rose",
      "gray"
    ]),
    tableHead: PropTypes.arrayOf(PropTypes.string),
    tableData: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string))
  };
  export default Dashboard
