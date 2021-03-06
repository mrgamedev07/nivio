import React from 'react';
import { Typography, AppBar, Theme, createStyles, Box, darken } from '@material-ui/core';
import { Link } from 'react-router-dom';

import Toolbar from '@material-ui/core/Toolbar';

import makeStyles from '@material-ui/core/styles/makeStyles';
import Search from '../Landscape/Search/Search';
import { HelpOutlineRounded } from '@material-ui/icons';
import IconButton from '@material-ui/core/IconButton';
import Button from '@material-ui/core/Button';
import Avatar from '@material-ui/core/Avatar';

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    grow: {
      flexGrow: 1,
    },
    pageTitle: {
      padding: 11,
      paddingLeft: 16,
      paddingRight: 16,
      backgroundColor: darken(theme.palette.primary.main, 0.2),
    },
    menuIcon: {
      color: 'rgba(255, 255, 255, 0.75)',
    },
    logo: {
      height: '1.5em',
    },
  })
);

interface Props {
  appBarClass: string;
  setSidebarContent: Function;
  pageTitle?: string;
  logo?: string;
}

/**
 * Header Component
 */
const Navigation: React.FC<Props> = ({ appBarClass, setSidebarContent, pageTitle, logo }) => {
  const classes = useStyles();

  return (
    <AppBar position='static' className={appBarClass}>
      <Toolbar variant='dense'>
        <Button component={Link} to={``} className={classes.menuIcon}>
          {logo ? (
            <Avatar
              className={classes.logo}
              imgProps={{ style: { objectFit: 'contain' } }}
              src={logo}
            />
          ) : (
            'nivio'
          )}
        </Button>
        <Box className={classes.pageTitle}>
          <Typography variant='h6'>{pageTitle}</Typography>
        </Box>
        <div className={classes.grow} />
        <Search setSidebarContent={setSidebarContent} />{' '}
        <IconButton
          className={classes.menuIcon}
          data-testid='ManualButton'
          component={Link}
          to={`/man/install.html`}
          title={'Help / Manual'}
        >
          <HelpOutlineRounded />
        </IconButton>
      </Toolbar>
    </AppBar>
  );
};

export default Navigation;
