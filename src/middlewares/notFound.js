const { error } = require('../utils/apiResponse');

const notFound = (req, res) => {
  return error(res, `Route ${req.originalUrl} not found`, 404);
};

module.exports = notFound;
