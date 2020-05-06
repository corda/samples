import React from "react";
import clsx from 'clsx';

import PropTypes from "prop-types";
// @material-ui/core components
import { makeStyles } from "@material-ui/core/styles";
import Table from "@material-ui/core/Table";
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import Button from "components/CustomButtons/Button.js";
import Modal from '@material-ui/core/Modal';
import Backdrop from '@material-ui/core/Backdrop';
import Fade from '@material-ui/core/Fade';
import Input from '@material-ui/core/Input';
import Grid from '@material-ui/core/Grid';

// core components
import styles from "assets/jss/material-dashboard-react/components/tableStyle.js";

const useStyles = makeStyles(theme => ({
  modal: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  paper: {
    backgroundColor: theme.palette.background.paper,
    border: '2px solid #000',
    boxShadow: theme.shadows[5],
    padding: theme.spacing(2, 4, 3),
  },
}));

export default function CustomTable(props) {
  const classes = useStyles();
  const [open, setOpen] = React.useState(false);

    const handleOpen = () => {
      setOpen(true);
    };

    const handleClose = () => {
      setOpen(false);
    };

    const sendflow = () => {
      setOpen(false);
    };
    const sendacceptflow = () => {
      fetch('http://localhost:3000/ping')
      .then(res => {
        console.log(res);
      })
      .catch(console.log)

    };
  const { tableHead, tableData, tableHeaderColor } = props;
  return (
    <div className={classes.tableResponsive}>
      <Table className={classes.table}>
        {tableHead !== undefined ? (
          <TableHead className={classes[tableHeaderColor + "TableHeader"]}>
            <TableRow className={classes.tableHeadRow}>
            <TableCell component="th" scope="row">Counter Party</TableCell>
              {tableHead.map((prop) => {
                return (
                  <TableCell align="right">{prop}</TableCell>
                );
              })}
            </TableRow>
          </TableHead>
        ) : null}

        <TableBody>
          {tableData.state.data.map((transaction) => {
            return (
              <TableRow className={classes.tableBodyRow}>
                <TableCell component="th" scope="row">{transaction.data.amount}</TableCell>
                <TableCell align="right">{transaction.data.amount}</TableCell>
                <TableCell align="right">{transaction.data.amount}</TableCell>
                <Button style={{float: 'right'}} color="white" onClick={handleOpen}>Modify</Button>
                <Modal
                  aria-labelledby="transition-modal-title"
                  aria-describedby="transition-modal-description"
                  className={classes.modal}
                  open={open}
                  onClose={handleClose}
                  closeAfterTransition
                  BackdropComponent={Backdrop}
                  BackdropProps={{
                    timeout: 500,
                  }}
                >
                  <Fade in={open}>
                    <div className={classes.paper}>
                      <h2 id="transition-modal-title">The original Proposal Amount is ${transaction.name} USD</h2>
                      <Grid container spacing={1}>
                      <Grid item xs={12} sm={3}>
                        <p id="transition-modal-description">Enter your desired amout: </p>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                      <Input
                        placeholder="new amout here"
                        className={classes.input}
                        inputProps={{
                          'aria-label': 'description',
                        }}
                      />
                        </Grid>
                        <Grid item xs={12} sm={3}>
                          <Button color="white" onClick={sendflow}>Propose Change</Button>
                        </Grid>
                      </Grid>
                    </div>
                  </Fade>
                </Modal>
                <Button style={{float: 'right'}} color="white" onClick={sendacceptflow} >Accept</Button>

              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}

CustomTable.defaultProps = {
  tableHeaderColor: "gray"
};

CustomTable.propTypes = {
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
