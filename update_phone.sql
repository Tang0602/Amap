UPDATE poi SET phone = '027-8' || substr('0000000' || (id % 10000000), -7) WHERE main_category = '景点' AND (phone IS NULL OR phone = '');
