const healthService = require('../services/healthService');
const { success } = require('../utils/apiResponse');

const checkHealth = (req, res) => {
  const data = healthService.getHealthStatus();

  return success(res, data, 'Backend is running');
};

module.exports = {
  checkHealth
};
