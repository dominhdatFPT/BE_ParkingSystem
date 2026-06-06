const express = require('express');
const cors = require('cors');
const morgan = require('morgan');

const env = require('./configs/env');
const routes = require('./routes');
const notFound = require('./middlewares/notFound');
const errorHandler = require('./middlewares/errorHandler');

const app = express();

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

if (env.nodeEnv === 'development') {
  app.use(morgan('dev'));
}

app.use(env.apiPrefix, routes);

app.use(notFound);
app.use(errorHandler);

module.exports = app;
